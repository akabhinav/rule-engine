package com.ruleengine.core;

import com.ruleengine.model.Rule;
import com.ruleengine.parser.JsonRuleParser;
import com.ruleengine.repository.RuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Hot Reload Mechanism
 *
 * Watches for rule changes and reloads them automatically
 * Supports:
 * - File system watching
 * - Periodic polling
 * - Manual reload triggers
 */
@Component
public class RuleReloader {

    private static final Logger logger = LoggerFactory.getLogger(RuleReloader.class);

    private final RuleRepository ruleRepository;
    private final JsonRuleParser ruleParser;
    private final RuleIndexer ruleIndexer;

    private final ScheduledExecutorService scheduler;
    private final Map<String, WatchKey> watchKeys;
    private WatchService watchService;

    public RuleReloader(
            RuleRepository ruleRepository,
            JsonRuleParser ruleParser,
            RuleIndexer ruleIndexer) {
        this.ruleRepository = ruleRepository;
        this.ruleParser = ruleParser;
        this.ruleIndexer = ruleIndexer;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.watchKeys = new HashMap<>();
    }

    /**
     * Start watching a directory for rule changes
     */
    public void watchDirectory(String directoryPath) throws IOException {
        Path dir = Paths.get(directoryPath);

        if (!Files.exists(dir)) {
            logger.warn("Directory does not exist: {}", directoryPath);
            return;
        }

        if (watchService == null) {
            watchService = FileSystems.getDefault().newWatchService();
        }

        WatchKey key = dir.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE
        );

        watchKeys.put(directoryPath, key);

        logger.info("Started watching directory: {}", directoryPath);

        // Start watch thread
        scheduler.submit(this::processWatchEvents);
    }

    /**
     * Process file system watch events
     */
    private void processWatchEvents() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    if (filename.toString().endsWith(".json")) {
                        handleFileChange(filename, kind);
                    }
                }

                if (!key.reset()) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.info("Watch service interrupted");
        }
    }

    private void handleFileChange(Path filename, WatchEvent.Kind<?> kind) {
        try {
            if (kind == StandardWatchEventKinds.ENTRY_CREATE ||
                kind == StandardWatchEventKinds.ENTRY_MODIFY) {

                logger.info("Detected change in rule file: {}", filename);
                reloadRuleFromFile(filename.toString());

            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                logger.info("Detected deletion of rule file: {}", filename);
                // Handle rule deletion if needed
            }
        } catch (Exception e) {
            logger.error("Error handling file change: " + filename, e);
        }
    }

    /**
     * Reload a rule from file
     */
    public void reloadRuleFromFile(String filePath) {
        try {
            Rule rule = ruleParser.parseFromFile(filePath);
            reloadRule(rule);
            logger.info("Successfully reloaded rule: {} from {}", rule.getName(), filePath);
        } catch (Exception e) {
            logger.error("Failed to reload rule from file: " + filePath, e);
        }
    }

    /**
     * Reload a single rule
     */
    public void reloadRule(Rule rule) {
        // Update in repository
        if (ruleRepository.findById(rule.getId()).isPresent()) {
            ruleRepository.update(rule);
            logger.info("Updated existing rule: {}", rule.getId());
        } else {
            ruleRepository.save(rule);
            logger.info("Added new rule: {}", rule.getId());
        }

        // Update index
        ruleIndexer.indexRule(rule);
    }

    /**
     * Reload all rules from repository
     */
    public void reloadAllRules() {
        logger.info("Reloading all rules...");
        List<Rule> rules = ruleRepository.findAll();

        // Rebuild index
        ruleIndexer.rebuildIndex(rules);

        logger.info("Reloaded {} rules", rules.size());
    }

    /**
     * Schedule periodic reload
     */
    public void schedulePeriodicReload(long intervalMinutes) {
        scheduler.scheduleAtFixedRate(
                this::reloadAllRules,
                intervalMinutes,
                intervalMinutes,
                TimeUnit.MINUTES
        );

        logger.info("Scheduled periodic reload every {} minutes", intervalMinutes);
    }

    /**
     * Stop watching
     */
    public void stopWatching() {
        watchKeys.values().forEach(WatchKey::cancel);
        watchKeys.clear();

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.error("Error closing watch service", e);
            }
        }

        scheduler.shutdown();
        logger.info("Stopped rule reloader");
    }

    /**
     * Get reload statistics
     */
    public ReloadStatistics getStatistics() {
        return new ReloadStatistics(
                watchKeys.size(),
                ruleRepository.count()
        );
    }

    public record ReloadStatistics(int watchedDirectories, long totalRules) {}
}
