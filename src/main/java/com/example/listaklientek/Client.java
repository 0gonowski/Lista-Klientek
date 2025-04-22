package com.example.listaklientek;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Client {
    private SimpleStringProperty name;
    private SimpleStringProperty phoneNumber;
    private SimpleDoubleProperty totalAmount;
    private SimpleDoubleProperty lastPurchaseAmount;
    private SimpleDoubleProperty discount;
    private double lastRedeemedThreshold = 0;
    private int purchaseCount = 0;
    private List<Double> discountThresholds;  // Lista wygenerowanych progów
    private boolean purchaseLockOnRabatPurchase = false;
    private double currentDiscountRate = 0.02;  // Zaczynamy od rabatu 2%

    // Przechowujemy dwie ostatnie wartości ciągu Fibonacciego
    private double previousFib = 0;
    private double currentFib = 1000;  // Pierwszy próg rabatowy to 1000 zł

    private final int PURCHASES_FOR_DISCOUNT = 5;

    public Client(String name, double lastPurchaseAmount, String phone, double lastRedeemedThreshold) {
        this.name = new SimpleStringProperty(name);
        this.phoneNumber = new SimpleStringProperty(phone);
        this.totalAmount = new SimpleDoubleProperty(lastPurchaseAmount);
        this.lastPurchaseAmount = new SimpleDoubleProperty(lastPurchaseAmount);
        this.discount = new SimpleDoubleProperty(calculateDiscount(lastPurchaseAmount));
        this.lastRedeemedThreshold = lastRedeemedThreshold;
        this.discountThresholds = new ArrayList<>();
        discountThresholds.add(currentFib);  // Dodajemy pierwszy próg
        this.purchaseCount = 0;
        this.purchaseLockOnRabatPurchase = false;
    }

    public Client(String name, double lastPurchaseAmount, String phone) {
        this(name, lastPurchaseAmount, phone, 0);
    }

    public String getPhoneNumber() {
        return phoneNumber.get();
    }

    public SimpleStringProperty phoneNumberProperty() {
        return phoneNumber;
    }

    public String getName() {
        return name.get();
    }

    public SimpleStringProperty nameProperty() {
        return name;
    }

    public double getTotalAmount() {
        return totalAmount.get();
    }

    public SimpleDoubleProperty totalAmountProperty() {
        return totalAmount;
    }

    public double getLastPurchaseAmount() {
        return lastPurchaseAmount.get();
    }

    public SimpleDoubleProperty lastPurchaseAmountProperty() {
        return lastPurchaseAmount;
    }

    public double getDiscount() {
        return discount.get();
    }

    public SimpleDoubleProperty discountProperty() {
        return discount;
    }

    public int getPurchaseCount() {
        return purchaseCount;
    }

    public double getLastRedeemedThreshold() {
        return lastRedeemedThreshold;
    }

    // Dodaj nowy zakup
    public void addPurchase(double purchaseAmount) {
        this.lastPurchaseAmount.set(purchaseAmount);
        this.totalAmount.set(totalAmount.get() + purchaseAmount);
        this.discount.set(calculateDiscount(purchaseAmount));  // Obliczamy rabat
        this.purchaseCount++;
        this.purchaseLockOnRabatPurchase = false;
    }

    // Sprawdź, czy klient kwalifikuje się do dużego rabatu
    public boolean isEligibleForBigDiscount() {
        generateNextFibonacciThresholdIfNeeded();
        double nextThreshold = getNextThreshold();
        return (totalAmount.get() >= nextThreshold && nextThreshold > lastRedeemedThreshold) || purchaseCount >= PURCHASES_FOR_DISCOUNT;
    }

    // Pobierz następny próg rabatowy
    public double getNextThreshold() {
        generateNextFibonacciThresholdIfNeeded();  // Upewnij się, że mamy wystarczająco dużo progów
        for (double threshold : discountThresholds) {
            if (threshold > lastRedeemedThreshold) {
                return threshold;
            }
        }
        return discountThresholds.get(discountThresholds.size() - 1);  // Zwróć największy próg, jeśli nie ma większych
    }

    // Odbierz duży rabat
    public double redeemBigDiscount() {
        if (isEligibleForBigDiscount()) {
            purchaseLockOnRabatPurchase = true; // Blokada cofania po przyznaniu rabatu

            // Obliczamy rabat zgodnie z zasadami
            double discountValue = calculateDiscount(lastPurchaseAmount.get());

            // Jeśli przekroczono próg, zaktualizuj próg, ale nie resetuj rabatu od razu
            if (totalAmount.get() >= getNextThreshold()) {
                lastRedeemedThreshold = getNextThreshold();  // Aktualizacja progu po odebraniu rabatu
                purchaseCount = 0;  // Resetuj liczbę zakupów po odebraniu rabatu
            }

            lastPurchaseAmount.set(0);  // Wyzeruj ostatni zakup

            // Zmień rabat na następny dopiero po odebraniu rabatu
            adjustDiscountRate();

            return discountValue;
        }
        return 0;
    }

    // Oblicz rabat, zaokrąglając go w zależności od kwoty
    private double calculateDiscount(double amount) {
        double discountValue = amount * currentDiscountRate;

        // Jeśli rabat wynosi mniej niż 50 groszy, ustawiamy go na 1 zł
        if (discountValue < 0.5) {
            return 1.0;
        } else if (discountValue >= 50) {
            return Math.ceil(discountValue);  // Zaokrąglamy w górę
        } else {
            return Math.floor(discountValue);  // Zaokrąglamy w dół
        }
    }

    // Zmień aktualny rabat na podstawie liczby przekroczonych progów
    private void adjustDiscountRate() {
        // Liczba progów, które zostały przekroczone od ostatniego odebrania rabatu
        long numberOfThresholdsCrossed = discountThresholds.stream()
                .filter(threshold -> totalAmount.get() >= threshold)
                .count();

        // Cykl rabatów: 2%, 5%, 10%, 5%, 15%, 20%
        double[] discountCycle = {0.02, 0.05, 0.10, 0.05, 0.15, 0.20};

        // Upewnij się, że liczba przekroczonych progów jest obliczana poprawnie i resetowana po odebraniu rabatu
        int cycleIndex = (int) ((numberOfThresholdsCrossed - 1) % discountCycle.length);  // Odjęcie 1, aby uwzględnić, że pierwszy próg to początek cyklu

        // Przypisanie rabatu zgodnie z indeksem cyklu
        currentDiscountRate = discountCycle[cycleIndex];
    }

    public void resetDiscountForNewYear() {
        if (LocalDate.now().getDayOfYear() == 1) {
            currentDiscountRate = 0.02;  // Reset rabatu na 2% w nowy rok
        }
    }

    public boolean undoLastPurchase(String currentUser) {
        if (purchaseLockOnRabatPurchase) {
            return false;
        }

        if (lastPurchaseAmount.get() > 0) {
            totalAmount.set(totalAmount.get() - lastPurchaseAmount.get());
            lastPurchaseAmount.set(0);
            purchaseCount = Math.max(0, purchaseCount - 1);
            discount.set(0);

            return true;
        }
        return false;
    }

    // Generuj kolejny próg Fibonacciego, jeśli jest potrzebny
    private void generateNextFibonacciThresholdIfNeeded() {
        // Sprawdzamy, czy ostatni próg jest już przekroczony
        if (totalAmount.get() >= currentFib) {
            // Generujemy kolejny próg Fibonacciego
            double nextFib = previousFib + currentFib;
            previousFib = currentFib;
            currentFib = nextFib;
            discountThresholds.add(currentFib);  // Dodajemy nowy próg do listy
        }
    }
}