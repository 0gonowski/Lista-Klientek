package com.example.listaklientek;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class ClientApp extends Application {

    private TableView<Client> table;
    private ObservableList<Client> clientList = FXCollections.observableArrayList();
    private final String FILE_NAME = "clients.csv";
    private final String ACTIVITY_LOG_FILE = "activity_log.txt";
    private final String LAST_CLEAN_FILE = "last_clean.txt";

    private String currentUser = ""; // Zmienna przechowująca zalogowanego użytkownika

    // Predefiniowane loginy i hasła
    private Map<String, String> validCredentials = new HashMap<>();

    @Override
    public void start(Stage primaryStage) {

        // Dodaj predefiniowane loginy i hasła
        validCredentials.put("x", "x");
        validCredentials.put("Ewa", "kot2312");
        validCredentials.put("Monika", "banan112");
        validCredentials.put("Edyta", "Serek0595");

        // Sprawdź, czy minęły dwa tygodnie od ostatniego czyszczenia logów
        checkAndCleanLogs();

        // Zresetuj rabaty klientów w nowy rok
        resetDiscountsForNewYear();

        // Wyświetl okno logowania
        showLoginDialog(primaryStage);
    }

    private void showLoginDialog(Stage primaryStage) {
        Stage loginStage = new Stage();
        loginStage.setTitle("Logowanie");

        Label loginLabel = new Label("Login:");
        TextField loginInput = new TextField();
        loginInput.setPromptText("Login");

        Label passwordLabel = new Label("Hasło:");
        PasswordField passwordInput = new PasswordField();
        passwordInput.setPromptText("Hasło");

        Button loginButton = new Button("Zaloguj");
        loginButton.setOnAction(e -> {
            String login = loginInput.getText();
            String password = passwordInput.getText();
            if (validateLogin(login, password)) {
                currentUser = login;
                logActivity("Zalogowano użytkownika: " + login);
                loginStage.close();
                showMainApp(primaryStage);
            } else {
                logActivity("Nieudana próba logowania dla loginu: " + login);
                showAlert("Błąd logowania", "Niepoprawny login lub hasło!");
            }
        });

        // Obsługa Enter dla logowania
        loginInput.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                loginButton.fire();
            }
        });
        passwordInput.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                loginButton.fire();
            }
        });

        // Obsługa Esc do zamknięcia okna logowania
        loginInput.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ESCAPE)) {
                loginStage.close();
            }
        });

        VBox loginLayout = new VBox(10, loginLabel, loginInput, passwordLabel, passwordInput, loginButton);
        loginLayout.setPadding(new Insets(10));
        Scene loginScene = new Scene(loginLayout, 300, 220);

        loginScene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());

        loginStage.setScene(loginScene);
        loginStage.show();
    }

    private boolean validateLogin(String login, String password) {
        return validCredentials.containsKey(login) && validCredentials.get(login).equals(password);
    }

    private void showMainApp(Stage primaryStage) {
        primaryStage.setTitle("Lista klientek");

        BorderPane layout = new BorderPane();
        VBox centerLayout = new VBox();
        centerLayout.setPadding(new Insets(10, 10, 10, 10));
        centerLayout.setSpacing(10);

        TableColumn<Client, Void> optionsColumn = new TableColumn<>("Opcje");
        optionsColumn.setMinWidth(100);
        optionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button optionsButton = new Button("...");

            {
                optionsButton.setOnAction(event -> {
                    Client client = getTableView().getItems().get(getIndex());
                    showClientOptions(client);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(optionsButton);
                }
            }
        });

        TableColumn<Client, String> nameColumn = new TableColumn<>("Imię i nazwisko");
        nameColumn.setMinWidth(200);
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());

        TableColumn<Client, String> phoneColumn = new TableColumn<>("Numer telefonu");
        phoneColumn.setMinWidth(150);
        phoneColumn.setCellValueFactory(data -> data.getValue().phoneNumberProperty());

        TableColumn<Client, String> totalAmountColumn = new TableColumn<>("Łączna kwota zakupów");
        totalAmountColumn.setMinWidth(150);
        totalAmountColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.2f zł", data.getValue().getTotalAmount()))
        );

        TableColumn<Client, String> lastPurchaseColumn = new TableColumn<>("Ostatni zakup");
        lastPurchaseColumn.setMinWidth(100);
        lastPurchaseColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.2f zł", data.getValue().getLastPurchaseAmount()))
        );

        TableColumn<Client, String> discountColumn = new TableColumn<>("Rabat");
        discountColumn.setMinWidth(100);
        discountColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.2f zł", data.getValue().getDiscount()))
        );

        TableColumn<Client, String> bigDiscountColumn = new TableColumn<>("Jednorazowy rabat");
        bigDiscountColumn.setCellValueFactory(data -> {
            if (data.getValue().isEligibleForBigDiscount()) {
                return new SimpleStringProperty("Do odebrania");
            } else {
                return new SimpleStringProperty("Niedostępny");
            }
        });

        TableColumn<Client, Void> addPurchaseColumn = new TableColumn<>("Dodaj zakup");
        addPurchaseColumn.setMinWidth(100);
        addPurchaseColumn.setCellFactory(param -> new TableCell<>() {
            private final Button addButton = new Button("+");

            {
                addButton.setOnAction(event -> {
                    Client client = getTableView().getItems().get(getIndex());
                    showAddPurchaseDialog(client);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(addButton);
                }
            }
        });

        TableColumn<Client, Void> redeemDiscountColumn = new TableColumn<>("Odbierz rabat");
        redeemDiscountColumn.setMinWidth(100);
        redeemDiscountColumn.setCellFactory(param -> new TableCell<>() {
            private final Button redeemButton = new Button("Odbierz");

            {
                redeemButton.setOnAction(event -> handleRedeemDiscount(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Client client = getTableView().getItems().get(getIndex());
                    if (client.isEligibleForBigDiscount()) {
                        setGraphic(redeemButton);
                        redeemButton.setDisable(false);
                    } else {
                        setGraphic(redeemButton);
                        redeemButton.setDisable(true);
                    }
                }
            }
        });

        table = new TableView<>();
        table.setItems(clientList);
        table.getColumns().addAll(optionsColumn, nameColumn, phoneColumn, addPurchaseColumn, totalAmountColumn, lastPurchaseColumn, discountColumn, bigDiscountColumn, redeemDiscountColumn);

        // Dodanie rowFactory z czarną obramówką tekstu dla wierszy z zielonym tłem
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Client client, boolean empty) {
                super.updateItem(client, empty);
                if (client == null || empty) {
                    setStyle("");
                    getStyleClass().remove("highlight-green");
                } else if (client.isEligibleForBigDiscount()) {
                    setStyle("-fx-background-color: lightgreen;");
                    getStyleClass().add("highlight-green"); // Dodanie klasy CSS
                } else {
                    setStyle("");
                    getStyleClass().remove("highlight-green");
                }
            }
        });

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // Ensure columns resize with window

        TextField searchField = new TextField();
        searchField.setPromptText("Szukaj klientki...");

        FilteredList<Client> filteredData = new FilteredList<>(clientList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(client -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                return client.getName().toLowerCase().contains(lowerCaseFilter)||
                        client.getPhoneNumber().toLowerCase().contains(lowerCaseFilter);
            });
        });

        SortedList<Client> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        // Ensure the table and its elements resize with the stage
        primaryStage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            table.setPrefWidth(newWidth.doubleValue() * 0.95);
        });

        primaryStage.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            table.setPrefHeight(newHeight.doubleValue() * 0.85);
        });

        TextField nameInput = new TextField();
        nameInput.setPromptText("Imię i nazwisko");

        // Dodanie walidacji: tylko litery i spacje, brak cyfr
        nameInput.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("[a-zA-ZĄĆĘŁŃÓŚŹŻąćęłńóśźż ]*")) {
                return change;
            }
            return null;
        }));

        TextField phoneInput = new TextField();
        phoneInput.setPromptText("Numer telefonu (opcjonalnie)");

// Dodanie walidacji: tylko cyfry
        phoneInput.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change;
            }
            return null;
        }));

        TextField amountInput = new TextField();
        amountInput.setPromptText("Kwota zakupu");

        // Usuwamy formatowanie z "zł"
        amountInput.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("-?\\d*(\\.\\d{0,2})?")) {
                return change;
            }
            return null;
        }));

        Button addButton = new Button("Dodaj klientkę");
        addButton.setOnAction(e -> {
            addClient(nameInput, phoneInput, amountInput);
        });

        Button registerButton = new Button("Rejestr");
        registerButton.setVisible(currentUser.equals("Edyta"));
        registerButton.setOnAction(e -> showActivityLog());

        HBox formLayout = new HBox();
        formLayout.setSpacing(10);
        formLayout.setPadding(new Insets(10, 10, 10, 10));
        formLayout.getChildren().addAll(nameInput, phoneInput, amountInput, addButton);

        HBox bottomLayout = new HBox(10);
        bottomLayout.setPadding(new Insets(10));
        bottomLayout.setAlignment(Pos.CENTER_RIGHT);
        bottomLayout.getChildren().add(registerButton);

        HBox topBottomLayout = new HBox(10, formLayout, bottomLayout);
        topBottomLayout.setSpacing(20);
        topBottomLayout.setPadding(new Insets(10));

        centerLayout.getChildren().addAll(searchField, table, topBottomLayout);

        layout.setCenter(centerLayout);

        loadClientsFromFile();

        Scene scene = new Scene(layout, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());

        primaryStage.setWidth(1200);
        primaryStage.setHeight(800);
        primaryStage.setResizable(true);
        primaryStage.setScene(scene);

        primaryStage.setFullScreen(false);
        primaryStage.show();
    }

    private void addClient(TextField nameInput, TextField phoneInput, TextField amountInput) {
        String name = nameInput.getText();
        String phone = phoneInput.getText();
        String amountText = amountInput.getText();

        // Walidacja numeru telefonu (pusty lub 9 cyfr)
        if (!phone.isEmpty() && !phone.matches("\\d{9}")) {
            showAlert("Błąd", "Numer telefonu musi zawierać 9 cyfr lub być pusty");
            return;
        }

        // Sprawdzenie, czy numer telefonu jest unikalny
        if (!phone.isEmpty() && !isPhoneNumberUnique(phone)) {
            showAlert("Błąd", "Numer telefonu już istnieje w bazie danych. Wprowadź inny numer.");
            return;
        }

        // Walidacja imienia i nazwiska
        if (!name.matches("[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż]+ [A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż]+")) {
            showAlert("Błąd", "Imię i nazwisko musi zawierać dwa człony, oba zaczynające się dużą literą");
            return;
        }

        if (amountText.isEmpty()) {
            showAlert("Błąd", "Kwota zakupu nie może być pusta");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            Client newClient = new Client(name, amount, phone);
            clientList.add(newClient);
            saveClientsToFile();
            table.refresh();
            logActivity("Dodano nową klientkę: " + name);
            nameInput.clear();
            phoneInput.clear();
            amountInput.clear();
        } catch (NumberFormatException e) {
            showAlert("Błąd", "Niepoprawna kwota zakupu");
        }
    }

    // Funkcja sprawdzająca, czy numer telefonu jest unikalny
    private boolean isPhoneNumberUnique(String phoneNumber) {
        for (Client client : clientList) {
            if (client.getPhoneNumber().equals(phoneNumber)) {
                return false;  // Numer telefonu jest już w użyciu
            }
        }
        return true;  // Numer telefonu jest unikalny
    }


    private void showAddPurchaseDialog(Client client) {
        Stage dialog = new Stage();
        dialog.setTitle("Dodaj zakup");

        Label label = new Label("Wprowadź kwotę zakupu:");
        TextField purchaseAmountInput = new TextField();
        purchaseAmountInput.setPromptText("Kwota");

        Button addButton = new Button("Dodaj");
        addButton.setOnAction(e -> {
            String amountText = purchaseAmountInput.getText();

            if (amountText.isEmpty()) {
                showAlert("Błąd", "Kwota nie może być pusta");
                return;
            }
            try {
                double newPurchaseAmount = Double.parseDouble(amountText);
                client.addPurchase(newPurchaseAmount);
                logActivity("Dodano zakup dla klientki: " + client.getName() + " na kwotę: " + newPurchaseAmount + " zł");
                saveClientsToFile();
                table.refresh();
                dialog.close();
            } catch (NumberFormatException ex) {
                showAlert("Błąd", "Niepoprawna kwota zakupu");
            }
        });

        // Obsługa Enter i Esc dla dialogu
        purchaseAmountInput.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ENTER)) {
                addButton.fire();
            }
        });

        Scene dialogScene = new Scene(new VBox(10, label, purchaseAmountInput, addButton), 300, 200);
        dialogScene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());

        dialogScene.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ESCAPE)) {
                dialog.close();
            }
        });

        dialog.initModality(Modality.APPLICATION_MODAL);  // Ustawienie jako modalne
        dialog.initOwner(table.getScene().getWindow());
        dialog.setScene(dialogScene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    private void handleRedeemDiscount(Client client) {
        if (client.isEligibleForBigDiscount()) {
            double discountValue = client.redeemBigDiscount();  // Odbieramy rabat i zmieniamy na kolejny
            logActivity("Rabat o wartości " + String.format("%.2f zł", discountValue) + " odebrano dla klientki: " + client.getName());
            saveClientsToFile();
            table.refresh();
        } else {
            showAlert("Błąd", "Klientka nie ma dostępnego rabatu do odebrania.");
        }
    }

    private void showClientOptions(Client client) {
        Stage optionsStage = new Stage();
        optionsStage.setTitle("Opcje klientki: " + client.getName());

        Button undoPurchaseButton = new Button("Cofnij ostatni zakup");
        undoPurchaseButton.setOnAction(e -> {
            if (client.undoLastPurchase(currentUser)) {
                logActivity(currentUser + " cofnął ostatni zakup dla klientki: " + client.getName());
                saveClientsToFile();
                table.refresh();
                optionsStage.close();
            } else {
                logActivity(currentUser + " próbował cofnąć zakup po wykorzystaniu rabatu dla klientki: " + client.getName());
                showUndoPurchaseError();
            }
        });

        Button deleteClientButton = new Button("Usuń klientkę");
        deleteClientButton.setOnAction(e -> {
            logActivity(currentUser + " usunął klientkę: " + client.getName());
            clientList.remove(client);
            saveClientsToFile();
            table.refresh();
            optionsStage.close();
        });

        Button discountInfoButton = new Button("Informacje o rabacie");
        discountInfoButton.setOnAction(e -> {
            showDiscountInfoDialog(client);
            optionsStage.close();
        });

        VBox optionsLayout = new VBox(10, undoPurchaseButton, deleteClientButton, discountInfoButton);
        optionsLayout.setPadding(new Insets(10));

        Scene optionsScene = new Scene(optionsLayout, 300, 250);
        optionsScene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());

        // Obsługa Esc w opcjach klienta
        optionsScene.setOnKeyPressed(e -> {
            if (e.getCode().equals(KeyCode.ESCAPE)) {
                optionsStage.close();
            }
        });

        optionsStage.setScene(optionsScene);
        optionsStage.initModality(Modality.APPLICATION_MODAL);
        optionsStage.initOwner(table.getScene().getWindow());
        optionsStage.setResizable(false);
        optionsStage.showAndWait();
    }

    private void showDiscountInfoDialog(Client client) {
        Stage infoDialog = new Stage();
        infoDialog.setTitle("Informacje o rabacie");

        Label totalAmountLabel = new Label("Łączna kwota zakupów: " + String.format("%.2f zł", client.getTotalAmount()));
        Label nextThresholdLabel = new Label("Następny próg rabatowy: " + String.format("%.2f zł", client.getNextThreshold()));
        Label purchaseCountLabel = new Label("Liczba zakupów: " + client.getPurchaseCount());

        Label discountLabel = new Label();
        if (client.isEligibleForBigDiscount()) {
            discountLabel.setText("Rabat dostępny: " + String.format("%.2f zł", client.getDiscount()));
            discountLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: green;");
        } else {
            discountLabel.setText("Rabat jeszcze niedostępny");
            discountLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: red;");
        }

        VBox dialogLayout = new VBox(10, totalAmountLabel, nextThresholdLabel, purchaseCountLabel, discountLabel);
        dialogLayout.setPadding(new Insets(10));

        Scene infoScene = new Scene(dialogLayout, 300, 200);
        infoScene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());

        infoDialog.setScene(infoScene);
        infoDialog.initModality(Modality.APPLICATION_MODAL);
        infoDialog.initOwner(table.getScene().getWindow());
        infoDialog.setResizable(false);
        infoDialog.showAndWait();
    }

    private void showUndoPurchaseError() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Cofnięcie zakupu zablokowane");
        alert.setHeaderText(null);
        alert.setContentText("Nie można cofnąć zakupu po przyznaniu rabatu.");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        alert.showAndWait();
    }

    private void logActivity(String activity) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ACTIVITY_LOG_FILE, true))) {
            String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.write(timeStamp + " - " + activity);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkAndCleanLogs() {
        try {
            Path lastCleanPath = Paths.get(LAST_CLEAN_FILE);
            if (!Files.exists(lastCleanPath)) {
                Files.writeString(lastCleanPath, LocalDateTime.now().toString());
                return;
            }
            LocalDateTime lastCleanDate = LocalDateTime.parse(Files.readString(lastCleanPath));
            LocalDateTime now = LocalDateTime.now();

            if (ChronoUnit.DAYS.between(lastCleanDate, now) >= 14) {
                Files.writeString(Paths.get(ACTIVITY_LOG_FILE), "");
                Files.writeString(lastCleanPath, now.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetDiscountsForNewYear() {
        for (Client client : clientList) {
            client.resetDiscountForNewYear();
        }
    }

    private void showActivityLog() {
        Stage logStage = new Stage();
        logStage.setTitle("Rejestr aktywności");

        TextArea logArea = new TextArea();
        logArea.setEditable(false);

        try {
            String logData = Files.readString(Paths.get(ACTIVITY_LOG_FILE));
            logArea.setText(logData);
        } catch (IOException e) {
            logArea.setText("Nie udało się wczytać rejestru aktywności.");
        }

        VBox logLayout = new VBox(10, logArea);
        logLayout.setPadding(new Insets(10));

        Scene logScene = new Scene(logLayout, 500, 400);
        logScene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());

        logStage.setScene(logScene);
        logStage.show();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        alert.showAndWait();
    }

    private void saveClientsToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Client client : clientList) {
                writer.write(client.getName() + "," + client.getPhoneNumber() + "," + client.getTotalAmount() + "," + client.getLastPurchaseAmount() + "," + client.getLastRedeemedThreshold());
                writer.newLine();
            }
        } catch (IOException e) {
            showAlert("Błąd zapisu", "Nie udało się zapisać danych do pliku");
        }
    }

    private void loadClientsFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length != 5) {
                    continue;
                }
                try {
                    String name = tokens[0];
                    String phone = tokens[1];
                    double totalAmount = Double.parseDouble(tokens[2]);
                    double lastPurchaseAmount = Double.parseDouble(tokens[3]);
                    double lastRedeemedThreshold = Double.parseDouble(tokens[4]);
                    Client client = new Client(name, lastPurchaseAmount, phone, lastRedeemedThreshold);
                    client.addPurchase(totalAmount - lastPurchaseAmount);
                    clientList.add(client);
                } catch (NumberFormatException e) {
                    showAlert("Błąd", "Niepoprawny format danych w pliku");
                }
            }
        } catch (IOException e) {
            showAlert("Błąd odczytu", "Nie udało się wczytać danych z pliku");
        }
    }
}