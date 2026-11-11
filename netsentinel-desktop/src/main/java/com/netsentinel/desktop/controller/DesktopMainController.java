package com.netsentinel.desktop.controller;

import com.netsentinel.desktop.collections.DataStore;
import com.netsentinel.desktop.model.DesktopActivityLog;
import com.netsentinel.desktop.model.DesktopEvent;
import com.netsentinel.desktop.model.DesktopIncident;
import com.netsentinel.desktop.model.DesktopStats;
import com.netsentinel.desktop.model.SimulationResult;
import com.netsentinel.desktop.service.NetSentinelApiClient;
import com.netsentinel.desktop.syllabus.AwtAdapterDemo;
import com.netsentinel.desktop.syllabus.LegacyCollectionsDemo;
import com.netsentinel.desktop.threading.EventBatchProcessor;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * =========================================================================
 * NETSENTINEL DESKTOP CONSOLE CONTROLLER
 * =========================================================================
 * 
 * Demonstrates:
 * 1. JavaFX Component Integration: TableView, ListView, TreeView, ComboBox, TextField, RadioButton, MenuBar, ToolBar
 * 2. Multithreading: JavaFX Task, background Thread, ExecutorService via EventBatchProcessor
 * 3. Safe UI updates with Platform.runLater()
 * 4. Collections & Generics integration with DataStore<DesktopIncident>
 * 5. Event Listeners, ChangeListeners, and KeyEvents
 * 6. Visual Effects (DropShadow, ScaleTransition) and Dynamic Dark/Light Theme Switching
 */
public class DesktopMainController {

    // -------------------------------------------------------------
    // FXML INJECTIONS: ToolBar & Header
    // -------------------------------------------------------------
    @FXML private MenuBar mainMenuBar;
    @FXML private Button toolbarRefreshBtn;
    @FXML private Button toolbarThemeBtn;
    @FXML private Button toolbarResetBtn;
    @FXML private Label connectionStatusLabel;
    @FXML private Label healthScoreBadge;
    @FXML private TextField quickSearchField;
    @FXML private Label threatIndexLabel;

    // -------------------------------------------------------------
    // FXML INJECTIONS: KPI Summary Metrics
    // -------------------------------------------------------------
    @FXML private Label totalActivitiesLabel;
    @FXML private Label totalIncidentsLabel;
    @FXML private Label highCriticalLabel;
    @FXML private Label openCasesLabel;
    @FXML private Label securityHealthLabel;
    @FXML private Label securityHealthStatusLabel;

    // -------------------------------------------------------------
    // FXML INJECTIONS: Navigation Tabs
    // -------------------------------------------------------------
    @FXML private TabPane mainTabPane;
    @FXML private Tab tabDashboard;
    @FXML private Tab tabIncidents;
    @FXML private Tab tabSimulation;
    @FXML private Tab tabExplorer;

    // -------------------------------------------------------------
    // FXML INJECTIONS: Tab 1 - Dashboard
    // -------------------------------------------------------------
    @FXML private ListView<String> dashboardActivityListView;
    @FXML private TextArea quickLogArea;

    // -------------------------------------------------------------
    // FXML INJECTIONS: Tab 2 - Incident Viewer
    // -------------------------------------------------------------
    @FXML private TextField incidentFilterField;
    @FXML private ComboBox<String> severityComboBox;
    @FXML private Button clearFilterBtn;
    @FXML private TableView<DesktopIncident> incidentsTableView;
    @FXML private TableColumn<DesktopIncident, String> colNumber;
    @FXML private TableColumn<DesktopIncident, String> colTitle;
    @FXML private TableColumn<DesktopIncident, String> colSeverity;
    @FXML private TableColumn<DesktopIncident, String> colStatus;
    @FXML private TableColumn<DesktopIncident, String> colActor;
    @FXML private TableColumn<DesktopIncident, String> colRule;
    @FXML private TableColumn<DesktopIncident, String> colCreated;

    @FXML private TextArea incidentDossierArea;
    @FXML private ListView<DesktopEvent> incidentTimelineListView;

    // -------------------------------------------------------------
    // FXML INJECTIONS: Tab 3 - Threat Simulator
    // -------------------------------------------------------------
    @FXML private ToggleGroup scenarioToggleGroup;
    @FXML private RadioButton radioSimAttendance;
    @FXML private RadioButton radioSimBulk;
    @FXML private RadioButton radioSimPrivilege;
    @FXML private RadioButton radioSimAccess;
    @FXML private ComboBox<String> simActorComboBox;
    @FXML private Button btnRunSimulation;
    @FXML private TextArea simulationOutputArea;

    // -------------------------------------------------------------
    // FXML INJECTIONS: Tab 4 - Event Explorer & Multithreading
    // -------------------------------------------------------------
    @FXML private TextField treeSearchField;
    @FXML private TreeView<String> eventTaxonomyTreeView;
    @FXML private Button btnRunBatch;
    @FXML private ProgressBar batchProgressBar;
    @FXML private TextArea batchOutputArea;

    // -------------------------------------------------------------
    // SERVICES & DATA STORES
    // -------------------------------------------------------------
    private final NetSentinelApiClient apiClient = new NetSentinelApiClient();

    // Demonstrates Custom Generic DataStore (using ArrayList, LinkedList, HashMap, HashSet, TreeSet, Iterator)
    private final DataStore<DesktopIncident> incidentStore = new DataStore<>(DesktopIncident::getIncidentNumber);

    // ObservableList for JavaFX TableView
    private final ObservableList<DesktopIncident> masterIncidentData = FXCollections.observableArrayList();
    private FilteredList<DesktopIncident> filteredIncidentData;

    // Multithreading demonstration processor
    private final EventBatchProcessor batchProcessor = new EventBatchProcessor();

    // Theme state
    private boolean isDarkTheme = true;

    // -------------------------------------------------------------
    // INITIALIZATION
    // -------------------------------------------------------------
    @FXML
    public void initialize() {
        setupTableColumns();
        setupFiltersAndSearch();
        setupSimulationControls();
        setupVisualEffects();
        setupEventProcessor();

        // Initial background sync
        handleRefresh();
    }

    private void setupTableColumns() {
        colNumber.setCellValueFactory(new PropertyValueFactory<>("incidentNumber"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colSeverity.setCellValueFactory(new PropertyValueFactory<>("severity"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colActor.setCellValueFactory(new PropertyValueFactory<>("actorUsername"));
        colRule.setCellValueFactory(new PropertyValueFactory<>("ruleCode"));
        colCreated.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // Wrap master data in FilteredList & SortedList
        filteredIncidentData = new FilteredList<>(masterIncidentData, p -> true);
        SortedList<DesktopIncident> sortedData = new SortedList<>(filteredIncidentData);
        sortedData.comparatorProperty().bind(incidentsTableView.comparatorProperty());
        incidentsTableView.setItems(sortedData);

        // Selection listener demonstrating ChangeListener
        incidentsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                displayIncidentDetails(newVal);
            }
        });
    }

    private void setupFiltersAndSearch() {
        // ComboBox setup
        severityComboBox.setItems(FXCollections.observableArrayList("ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW"));
        severityComboBox.getSelectionModel().select("ALL");

        // ChangeListener on ComboBox
        severityComboBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> applyIncidentFilters());

        // ChangeListener on Filter TextField
        incidentFilterField.textProperty().addListener((obs, o, n) -> applyIncidentFilters());

        // TreeView search listener
        treeSearchField.textProperty().addListener((obs, o, n) -> filterTreeView(n));
    }

    private void setupSimulationControls() {
        simActorComboBox.setItems(FXCollections.observableArrayList(
                "student101", "faculty101", "faculty102", "student102", "admin01"
        ));
        simActorComboBox.getSelectionModel().select("student101");
    }

    private void setupVisualEffects() {
        // Demonstrates JavaFX DropShadow effect on buttons
        DropShadow shadow = new DropShadow(6.0, Color.color(0, 0, 0, 0.4));
        btnRunSimulation.setEffect(shadow);

        // Demonstrates JavaFX ScaleTransition (interactive transform)
        btnRunSimulation.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), btnRunSimulation);
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
        });
        btnRunSimulation.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), btnRunSimulation);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    private void setupEventProcessor() {
        // Inter-thread communication listener
        batchProcessor.addSummaryListener(summary -> {
            batchProgressBar.setProgress(1.0);
            StringBuilder sb = new StringBuilder();
            sb.append(">>> BATCH PROCESSOR COMPLETED <<<\n");
            sb.append(summary.toString()).append("\n\n");
            sb.append("Summary details dispatched safely to JavaFX Application Thread via Platform.runLater().");
            batchOutputArea.setText(sb.toString());
            logQuickMessage("Multithreaded batch processing completed: " + summary.getTotalItems() + " events analyzed.");
        });
    }

    // -------------------------------------------------------------
    // DATA SYNC & REFRESH (Multithreaded via JavaFX Task)
    // -------------------------------------------------------------
    @FXML
    public void handleRefresh() {
        logQuickMessage("Initiating synchronization with Spring Boot Core...");
        connectionStatusLabel.setText("● SYNCING CORE...");
        connectionStatusLabel.setStyle("-fx-text-fill: #E6A23C; -fx-font-weight: bold; -fx-font-size: 11px;");

        Task<Void> syncTask = new Task<>() {
            @Override
            protected Void call() {
                boolean online = apiClient.checkHealth();
                DesktopStats stats = apiClient.fetchStats();
                List<DesktopIncident> incidents = apiClient.fetchIncidents();
                List<DesktopActivityLog> activityLogs = apiClient.fetchActivityLogs(30);

                Platform.runLater(() -> {
                    updateConnectionStatus(online);
                    updateKpiPanels(stats);
                    updateIncidentStore(incidents);
                    updateActivityTicker(activityLogs);
                    buildTaxonomyTree(activityLogs);
                    logQuickMessage("Core synchronization completed. " + incidents.size() + " incidents retrieved.");
                });
                return null;
            }
        };

        syncTask.setOnFailed(e -> Platform.runLater(() -> {
            connectionStatusLabel.setText("○ CORE OFFLINE (PORT 8080)");
            connectionStatusLabel.setStyle("-fx-text-fill: #E05252; -fx-font-weight: bold; -fx-font-size: 11px;");
            logQuickMessage("Sync failed: Core at http://localhost:8080 is unreachable.");
        }));

        new Thread(syncTask, "NetSentinel-SyncTask").start();
    }

    private void updateConnectionStatus(boolean online) {
        if (online) {
            connectionStatusLabel.setText("● CORE ONLINE (SYNCED)");
            connectionStatusLabel.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-font-size: 11px;");
        } else {
            connectionStatusLabel.setText("○ CORE OFFLINE (CHECK PORT 8080)");
            connectionStatusLabel.setStyle("-fx-text-fill: #E05252; -fx-font-weight: bold; -fx-font-size: 11px;");
        }
    }

    private void updateKpiPanels(DesktopStats stats) {
        totalActivitiesLabel.setText(String.valueOf(stats.getTotalActivities()));
        totalIncidentsLabel.setText(String.valueOf(stats.getTotalIncidents()));
        highCriticalLabel.setText(String.valueOf(stats.getHighCriticalIncidents()));
        openCasesLabel.setText(String.valueOf(stats.getOpenIncidents()));

        int health = stats.getHealthScore() > 0 ? stats.getHealthScore() : 95;
        String status = stats.getHealthStatus() != null ? stats.getHealthStatus() : "EXCELLENT";

        securityHealthLabel.setText(health + "%");
        securityHealthStatusLabel.setText("Status: " + status);
        healthScoreBadge.setText("🛡️ Health: " + health + "% (" + status + ")");

        int threat = stats.getThreatIndex() > 0 ? stats.getThreatIndex() : Math.max(0, 100 - health);
        threatIndexLabel.setText("THREAT INDEX: " + threat + " / 100");
    }

    private void updateIncidentStore(List<DesktopIncident> incidents) {
        // Demonstrates DataStore (ArrayList, LinkedList, HashMap, HashSet)
        incidentStore.clear();
        incidentStore.addAll(incidents);

        masterIncidentData.setAll(incidents);
        if (!incidents.isEmpty() && incidentsTableView.getSelectionModel().getSelectedItem() == null) {
            incidentsTableView.getSelectionModel().select(0);
        }
    }

    private void updateActivityTicker(List<DesktopActivityLog> activityLogs) {
        ObservableList<String> items = FXCollections.observableArrayList();
        for (DesktopActivityLog log : activityLogs) {
            String badge = "ALLOWED".equalsIgnoreCase(log.getOutcome()) ? "✓" : "✗";
            items.add(String.format("%s [%s] %s (%s) → %s | %s",
                    badge, log.getTimestamp() != null ? log.getTimestamp().replace("T", " ") : "",
                    log.getActorUsername(), log.getActorRole(), log.getAction(), log.getTargetResource()));
        }
        dashboardActivityListView.setItems(items);
    }

    // -------------------------------------------------------------
    // INCIDENT DETAILS & TIMELINE STREAMING
    // -------------------------------------------------------------
    private void displayIncidentDetails(DesktopIncident inc) {
        // 1. Build Sentinel Insight Dossier Text
        StringBuilder sb = new StringBuilder();
        sb.append("=================================================================\n");
        sb.append("NETSENTINEL INCIDENT DOSSIER: ").append(inc.getIncidentNumber()).append("\n");
        sb.append("=================================================================\n\n");
        sb.append("Title:       ").append(inc.getTitle()).append("\n");
        sb.append("Severity:    [").append(inc.getSeverity()).append("]\n");
        sb.append("Status:      [").append(inc.getStatus()).append("]\n");
        sb.append("Category:    ").append(inc.getCategory()).append("\n");
        sb.append("Rule Code:   ").append(inc.getRuleCode()).append("\n");
        sb.append("Confidence:  ").append(inc.getConfidenceScore()).append("%\n");
        sb.append("Actor:       ").append(inc.getActorUsername()).append(" (Role: ").append(inc.getActorRole()).append(")\n");
        sb.append("Target:      ").append(inc.getTargetResource()).append("\n");
        sb.append("Created:     ").append(inc.getCreatedAt()).append("\n\n");
        sb.append("--- [1] REASON FOR DETECTION ---\n");
        sb.append(inc.getReason()).append("\n\n");
        sb.append("--- [2] FORENSIC EVIDENCE PAYLOAD ---\n");
        sb.append(inc.getEvidenceJson()).append("\n\n");
        sb.append("--- [3] SENTINEL INSIGHT™ EXPLAINABLE INTELLIGENCE ---\n");
        sb.append(inc.getInsightExplanation()).append("\n\n");
        sb.append("--- [4] RECOMMENDED INVESTIGATION & MITIGATION STEPS ---\n");
        sb.append(inc.getRecommendedAction()).append("\n");

        incidentDossierArea.setText(sb.toString());

        // 2. Multithreaded fetch of Chronological Event Timeline
        if (inc.getId() != null) {
            Task<List<DesktopEvent>> eventTask = new Task<>() {
                @Override
                protected List<DesktopEvent> call() {
                    return apiClient.fetchIncidentEvents(inc.getId());
                }
            };

            eventTask.setOnSucceeded(e -> {
                List<DesktopEvent> events = eventTask.getValue();
                incidentTimelineListView.setItems(FXCollections.observableArrayList(events));
            });

            new Thread(eventTask, "NetSentinel-EventTimelineTask").start();
        }
    }

    // -------------------------------------------------------------
    // SEARCH & FILTERING (Syllabus Demonstration: Lambda, Predicate)
    // -------------------------------------------------------------
    private void applyIncidentFilters() {
        String filterText = incidentFilterField.getText();
        String selectedSeverity = severityComboBox.getSelectionModel().getSelectedItem();

        filteredIncidentData.setPredicate(inc -> {
            if (inc == null) return false;

            // Severity filter
            if (selectedSeverity != null && !"ALL".equalsIgnoreCase(selectedSeverity)) {
                if (!selectedSeverity.equalsIgnoreCase(inc.getSeverity())) {
                    return false;
                }
            }

            // Text search filter
            if (filterText == null || filterText.isBlank()) {
                return true;
            }

            String lower = filterText.toLowerCase().trim();
            return (inc.getIncidentNumber() != null && inc.getIncidentNumber().toLowerCase().contains(lower)) ||
                   (inc.getTitle() != null && inc.getTitle().toLowerCase().contains(lower)) ||
                   (inc.getActorUsername() != null && inc.getActorUsername().toLowerCase().contains(lower)) ||
                   (inc.getRuleCode() != null && inc.getRuleCode().toLowerCase().contains(lower));
        });
    }

    @FXML
    public void handleClearIncidentFilter() {
        incidentFilterField.clear();
        severityComboBox.getSelectionModel().select("ALL");
    }

    @FXML
    public void handleQuickSearchKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            String query = quickSearchField.getText();
            incidentFilterField.setText(query);
            mainTabPane.getSelectionModel().select(tabIncidents);
        }
    }

    // -------------------------------------------------------------
    // THREAT SIMULATION CONSOLE
    // -------------------------------------------------------------
    @FXML
    public void handleRunSelectedSimulation() {
        String scenario;
        if (radioSimAttendance.isSelected()) {
            scenario = "UNAUTHORIZED_ATTENDANCE";
        } else if (radioSimBulk.isSelected()) {
            scenario = "BULK_ACADEMIC_MODIFICATION";
        } else if (radioSimPrivilege.isSelected()) {
            scenario = "UNAUTHORIZED_PRIVILEGE_CHANGE";
        } else {
            scenario = "UNAUTHORIZED_STUDENT_RECORD_ACCESS";
        }

        String actor = simActorComboBox.getSelectionModel().getSelectedItem();
        if (actor == null || actor.isBlank()) actor = "student101";

        executeSimulationAsync(scenario, actor);
    }

    @FXML
    public void handleRunAllSimulations() {
        mainTabPane.getSelectionModel().select(tabSimulation);
        executeSimulationAsync("UNAUTHORIZED_ATTENDANCE", "student101");
    }

    private void executeSimulationAsync(String scenario, String actor) {
        simulationOutputArea.setText(">>> INJECTING THREAT VECTOR: " + scenario + " (Actor: " + actor + ")\nConnecting to Spring Boot security engine...");
        btnRunSimulation.setDisable(true);

        Task<SimulationResult> simTask = new Task<>() {
            @Override
            protected SimulationResult call() {
                return apiClient.triggerSimulation(scenario, actor);
            }
        };

        simTask.setOnSucceeded(e -> {
            btnRunSimulation.setDisable(false);
            SimulationResult res = simTask.getValue();
            if (res != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("=================================================================\n");
                sb.append(">>> ADVERSARY ATTACK SIMULATION COMPLETED <<<\n");
                sb.append("=================================================================\n\n");
                sb.append("Interception Status:   ").append(res.isDetected() ? "BLOCKED / ANOMALY FLAGGED" : "ALLOWED").append("\n");
                sb.append("Incident Reference:    ").append(res.getIncidentNumber()).append("\n");
                sb.append("Rule Intercepted:      ").append(res.getRuleCode()).append("\n");
                sb.append("Severity Assignment:   ").append(res.getSeverity()).append("\n");
                sb.append("Confidence Level:      ").append(res.getConfidenceScore()).append("%\n\n");
                sb.append("Detection Explanation:\n").append(res.getReason()).append("\n\n");
                sb.append("Sentinel Insight™ Heuristics:\n").append(res.getInsightExplanation()).append("\n\n");
                sb.append("Forensic Evidence JSON:\n").append(res.getEvidenceJson()).append("\n");

                simulationOutputArea.setText(sb.toString());
                logQuickMessage("Threat simulation executed: " + scenario + " -> " + res.getIncidentNumber());

                // Refresh dashboard to display the newly generated incident
                handleRefresh();
            } else {
                simulationOutputArea.setText("Simulation failed: Core engine did not respond.");
            }
        });

        simTask.setOnFailed(e -> {
            btnRunSimulation.setDisable(false);
            simulationOutputArea.setText("Error executing simulation: " + simTask.getException().getMessage());
        });

        new Thread(simTask, "NetSentinel-SimWorker").start();
    }

    // -------------------------------------------------------------
    // EVENT EXPLORER: TreeView & Taxonomy (Syllabus: TreeView, TreeItem)
    // -------------------------------------------------------------
    private void buildTaxonomyTree(List<DesktopActivityLog> logs) {
        TreeItem<String> rootItem = new TreeItem<>("🛡️ Campus Network Telemetry Root");
        rootItem.setExpanded(true);

        Map<String, List<DesktopActivityLog>> categoryMap = new HashMap<>();
        for (DesktopActivityLog log : logs) {
            String cat = log.getTargetType() != null ? log.getTargetType() : "GENERAL_RESOURCE";
            categoryMap.computeIfAbsent(cat, k -> new ArrayList<>()).add(log);
        }

        for (Map.Entry<String, List<DesktopActivityLog>> entry : categoryMap.entrySet()) {
            TreeItem<String> catItem = new TreeItem<>(entry.getKey() + " (" + entry.getValue().size() + " events)");

            for (DesktopActivityLog log : entry.getValue()) {
                String label = String.format("%s | %s (%s) → %s",
                        log.getOutcome(), log.getActorUsername(), log.getAction(), log.getTargetResource());
                catItem.getChildren().add(new TreeItem<>(label));
            }
            rootItem.getChildren().add(catItem);
        }

        eventTaxonomyTreeView.setRoot(rootItem);
    }

    private void filterTreeView(String query) {
        if (query == null || query.isBlank()) {
            return;
        }
        // Expand matching nodes
        if (eventTaxonomyTreeView.getRoot() != null) {
            expandMatching(eventTaxonomyTreeView.getRoot(), query.toLowerCase());
        }
    }

    private boolean expandMatching(TreeItem<String> item, String query) {
        boolean match = item.getValue() != null && item.getValue().toLowerCase().contains(query);
        for (TreeItem<String> child : item.getChildren()) {
            if (expandMatching(child, query)) {
                match = true;
            }
        }
        if (match) {
            item.setExpanded(true);
        }
        return match;
    }

    @FXML
    public void handleExpandTree() {
        if (eventTaxonomyTreeView.getRoot() != null) {
            expandAll(eventTaxonomyTreeView.getRoot(), true);
        }
    }

    private void expandAll(TreeItem<String> item, boolean expand) {
        item.setExpanded(expand);
        for (TreeItem<String> child : item.getChildren()) {
            expandAll(child, expand);
        }
    }

    // -------------------------------------------------------------
    // MULTITHREADED EVENT PROCESSOR DEMONSTRATION
    // -------------------------------------------------------------
    @FXML
    public void handleRunBatchProcessor() {
        btnRunBatch.setDisable(true);
        batchProgressBar.setProgress(0.25);
        batchOutputArea.setText("Starting multithreaded EventBatchProcessor...\nEnqueuing telemetry stream into thread-safe LinkedBlockingQueue...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                List<DesktopActivityLog> logs = apiClient.fetchActivityLogs(50);
                if (logs.isEmpty()) {
                    // Generate synthetic logs if offline for syllabus demo
                    logs = new ArrayList<>();
                    for (int i = 1; i <= 25; i++) {
                        DesktopActivityLog l = new DesktopActivityLog();
                        l.setId((long) i);
                        l.setActorUsername("sim_worker_" + (i % 4));
                        l.setAction(i % 5 == 0 ? "ATTENDANCE_RECORD_UNAUTHORIZED" : "MARKS_RECORD");
                        l.setOutcome(i % 5 == 0 ? "DENIED" : "ALLOWED");
                        logs.add(l);
                    }
                }

                List<DesktopActivityLog> batch = logs;
                batchProcessor.enqueueBatch(batch, () -> {
                    batchProgressBar.setProgress(0.65);
                    batchOutputArea.appendText("\nBatch successfully enqueued! Consumer worker thread actively processing items in background...");
                    btnRunBatch.setDisable(false);
                });
                return null;
            }
        };

        new Thread(task, "NetSentinel-BatchProducer").start();
    }

    // -------------------------------------------------------------
    // MENU BAR & VIEW ACTIONS
    // -------------------------------------------------------------
    @FXML
    public void handleToggleTheme() {
        Stage stage = (Stage) mainTabPane.getScene().getWindow();
        if (stage == null || stage.getScene() == null) return;

        isDarkTheme = !isDarkTheme;
        stage.getScene().getStylesheets().clear();

        if (isDarkTheme) {
            stage.getScene().getStylesheets().add(getClass().getResource("/css/desktop.css").toExternalForm());
            toolbarThemeBtn.setText("🌓 Light Mode");
            logQuickMessage("Switched UI theme to DARK MODE.");
        } else {
            stage.getScene().getStylesheets().add(getClass().getResource("/css/desktop-light.css").toExternalForm());
            toolbarThemeBtn.setText("🌓 Dark Mode");
            logQuickMessage("Switched UI theme to LIGHT MODE.");
        }
    }

    @FXML
    public void handleResetDemo() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Reset all simulation runs and restore default seeded state?", ButtonType.YES, ButtonType.NO);
        alert.setHeaderText("NetSentinel Demo State Reset");
        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.YES) {
                Task<Boolean> resetTask = new Task<>() {
                    @Override
                    protected Boolean call() {
                        return apiClient.resetDemoData();
                    }
                };
                resetTask.setOnSucceeded(e -> {
                    logQuickMessage("Demo state successfully reset to initial baseline.");
                    handleRefresh();
                });
                new Thread(resetTask).start();
            }
        });
    }

    @FXML
    public void handleExportDossier() {
        DesktopIncident selected = incidentsTableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Please select an incident from the table first.");
            a.show();
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Export Incident Dossier");
        fc.setInitialFileName(selected.getIncidentNumber() + "-dossier.txt");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Dossier (*.txt)", "*.txt"));

        File file = fc.showSaveDialog(mainTabPane.getScene().getWindow());
        if (file != null) {
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(incidentDossierArea.getText());
                logQuickMessage("Incident dossier exported to: " + file.getName());
            } catch (IOException e) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Error exporting file: " + e.getMessage());
                a.show();
            }
        }
    }

    @FXML
    public void handleClearCache() {
        incidentStore.clear();
        masterIncidentData.clear();
        logQuickMessage("Local in-memory cache cleared.");
    }

    @FXML
    public void handleSelectDashboardTab() { mainTabPane.getSelectionModel().select(tabDashboard); }

    @FXML
    public void handleSelectIncidentsTab() { mainTabPane.getSelectionModel().select(tabIncidents); }

    @FXML
    public void handleSelectSimulationTab() { mainTabPane.getSelectionModel().select(tabSimulation); }

    @FXML
    public void handleSelectExplorerTab() { mainTabPane.getSelectionModel().select(tabExplorer); }

    @FXML
    public void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About NetSentinel");
        alert.setHeaderText("NetSentinel — Desktop Security Operations Console v1.0");
        alert.setContentText("""
                NetSentinel: Intelligent Campus Academic Portal Security and Incident Analyzer.
                
                Architecture:
                - Java 21 LTS
                - JavaFX 21 (Desktop Console Client)
                - Spring Boot 3.3.4 (Security Heuristics Core)
                - SQLite with WAL Mode & JDBC
                - OpenPDF Report Generation
                
                Developed for Java Semester Academic Project Presentation.
                """);
        alert.show();
    }

    @FXML
    public void handleSyllabusMapping() {
        // Run syllabus demonstrator to verify in console
        LegacyCollectionsDemo.demonstrateVector(List.of("RULE-ATT-001", "RULE-MOD-002", "RULE-PRV-003"));
        LegacyCollectionsDemo.demonstrateHashtable(Map.of("Status", "ACTIVE", "Health", "95%"));

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Java Syllabus Concept Mapping");
        alert.setHeaderText("NetSentinel Academic Syllabus Coverage");
        alert.setContentText("""
                1. Object-Oriented Design: Polymorphism, BaseDetectionRule template pattern, Encapsulation.
                2. Exception Handling: Custom hierarchy (UnauthorizedAccessException, SecurityViolationException).
                3. Multithreading: ExecutorService, Thread, Runnable, BlockingQueue, synchronized locks.
                4. Collections: ArrayList, LinkedList, HashSet, HashMap, TreeSet, Iterator traversal.
                5. Generics: Generic interfaces, classes (DataStore<T>), methods, and wildcards (? extends / super).
                6. JavaFX UI: MenuBar, ToolBar, TableView, ListView, TreeView, RadioButton, KeyEvents, Effects.
                7. Legacy Syllabus Demos: Vector, Hashtable, Enumeration, AWT Adapters (com.netsentinel.desktop.syllabus).
                """);
        alert.show();
    }

    @FXML
    public void handleExit() {
        batchProcessor.shutdown();
        Platform.exit();
        System.exit(0);
    }

    private void logQuickMessage(String msg) {
        quickLogArea.appendText("[" + new java.util.Date().toString().substring(11, 19) + "] " + msg + "\n");
    }
}
