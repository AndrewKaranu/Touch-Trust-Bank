package com.touchtrust.touchtrustbank.Services;

import com.touchtrust.touchtrustbank.Models.DatabaseDriver;
import com.touchtrust.touchtrustbank.Models.Model;
import com.touchtrust.touchtrustbank.Models.Transaction;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RAGService {
    private final DatabaseDriver databaseDriver;
    // Simple embeddings for banking keywords (in a real implementation, use a proper embedding model)
    private final Map<String, List<String>> keywordToTopics;

    public RAGService(DatabaseDriver databaseDriver) {
        this.databaseDriver = databaseDriver;
        this.keywordToTopics = initializeKeywordMappings();
    }

    private Map<String, List<String>> initializeKeywordMappings() {
        Map<String, List<String>> mappings = new HashMap<>();

        // Balance-related keywords
        List<String> balanceKeywords = List.of("balance", "available", "funds", "money", "account");
        mappings.put("balance_query", balanceKeywords);

        // Transaction-related keywords
        List<String> transactionKeywords = List.of("spend", "spent", "payment", "transaction",
                "transfer", "history", "recent");
        mappings.put("transaction_query", transactionKeywords);

        // Credit-related keywords
        List<String> creditKeywords = List.of("credit", "limit", "card", "payment", "due",
                "minimum", "interest", "apr");
        mappings.put("credit_query", creditKeywords);

        // Savings-related keywords
        List<String> savingsKeywords = List.of("savings", "save", "interest", "withdraw", "limit");
        mappings.put("savings_query", savingsKeywords);

        return mappings;
    }

    public String getAnswer(String query) {
        // 1. Retrieve relevant context based on the query
        Map<String, Object> context = retrieveContext(query);

        // 2. Generate answer using the context
        return generateAnswer(query, context);
    }

    private Map<String, Object> retrieveContext(String query) {
        Map<String, Object> context = new HashMap<>();
        query = query.toLowerCase();

        // Identify the query type
        List<String> matchedTopics = identifyTopics(query);

        // Add client info to the context
        context.put("client_name", Model.getInstance().getClient().getFirstName() + " " +
                Model.getInstance().getClient().getLastName());
        context.put("client_address", Model.getInstance().getClient().getPayeeAddress());

        // Add relevant account information based on matched topics
        if (matchedTopics.contains("balance_query")) {
            addBalanceInfo(context);
        }

        if (matchedTopics.contains("transaction_query")) {
            addTransactionInfo(context, query);
        }

        if (matchedTopics.contains("credit_query") && Model.getInstance().getClient().hasCreditAccount()) {
            addCreditInfo(context);
        }

        if (matchedTopics.contains("savings_query")) {
            addSavingsInfo(context);
        }

        return context;
    }

    private List<String> identifyTopics(String query) {
        List<String> matchedTopics = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : keywordToTopics.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (query.contains(keyword)) {
                    matchedTopics.add(entry.getKey());
                    break;
                }
            }
        }

        return matchedTopics;
    }

    private void addBalanceInfo(Map<String, Object> context) {
        context.put("checking_balance", Model.getInstance().getClient().getCheckingAccount().balanceProperty().get());
        context.put("savings_balance", Model.getInstance().getClient().getSavingsAccount().balanceProperty().get());

        if (Model.getInstance().getClient().hasCreditAccount()) {
            context.put("credit_balance", Model.getInstance().getClient().getCreditAccount().balanceProperty().get());
            context.put("available_credit", Model.getInstance().getClient().getCreditAccount().availableCreditProperty().get());
        }
    }

    private void addTransactionInfo(Map<String, Object> context, String query) {
        // Determine time period from query
        int days = 30; // default
        if (query.contains("week") || query.contains("7 days")) {
            days = 7;
        } else if (query.contains("today") || query.contains("24 hours")) {
            days = 1;
        } else if (query.contains("year")) {
            days = 365;
        }

        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Transaction> allTransactions = Model.getInstance().getAllTransactions();
        List<Transaction> recentTransactions = new ArrayList<>();
        double totalSpent = 0;
        double totalReceived = 0;

        String clientAddress = Model.getInstance().getClient().getPayeeAddress();

        for (Transaction transaction : allTransactions) {
            if (transaction.dateProperty().get().atStartOfDay().isAfter(startDate)) {
                recentTransactions.add(transaction);

                if (transaction.senderProperty().get().equals(clientAddress)) {
                    totalSpent += transaction.amountProperty().get();
                } else if (transaction.receiverProperty().get().equals(clientAddress)) {
                    totalReceived += transaction.amountProperty().get();
                }
            }
        }

        context.put("days_period", days);
        context.put("total_transactions", recentTransactions.size());
        context.put("total_spent", totalSpent);
        context.put("total_received", totalReceived);
        context.put("net_change", totalReceived - totalSpent);

        // Add most recent 5 transactions for detailed information
        List<Map<String, Object>> recentTransactionDetails = new ArrayList<>();
        recentTransactions.stream()
                .sorted((t1, t2) -> t2.dateProperty().get().compareTo(t1.dateProperty().get()))
                .limit(5)
                .forEach(transaction -> {
                    Map<String, Object> transactionDetail = new HashMap<>();
                    transactionDetail.put("date", transaction.dateProperty().get().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    transactionDetail.put("amount", transaction.amountProperty().get());
                    transactionDetail.put("sender", transaction.senderProperty().get());
                    transactionDetail.put("receiver", transaction.receiverProperty().get());
                    transactionDetail.put("message", transaction.messageProperty().get());
                    transactionDetail.put("type", transaction.typeProperty().get());

                    recentTransactionDetails.add(transactionDetail);
                });

        context.put("recent_transactions", recentTransactionDetails);
    }

    private void addCreditInfo(Map<String, Object> context) {
        if (!Model.getInstance().getClient().hasCreditAccount()) {
            return;
        }

        context.put("credit_balance", Model.getInstance().getClient().getCreditAccount().balanceProperty().get());
        context.put("credit_limit", Model.getInstance().getClient().getCreditAccount().creditLimitProperty().get());
        context.put("available_credit", Model.getInstance().getClient().getCreditAccount().availableCreditProperty().get());
        context.put("minimum_payment", Model.getInstance().getClient().getCreditAccount().minimumPaymentProperty().get());
        context.put("payment_due_date", Model.getInstance().getClient().getCreditAccount().paymentDueDateProperty().get().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        context.put("apr", Model.getInstance().getClient().getCreditAccount().aprProperty().get() * 100);
    }

    private void addSavingsInfo(Map<String, Object> context) {
        context.put("savings_balance", Model.getInstance().getClient().getSavingsAccount().balanceProperty().get());
        context.put("withdrawal_limit", Model.getInstance().getClient().getSavingsAccount().withdrawalLimitProp().get());
        context.put("interest_rate", Model.getInstance().getClient().getSavingsAccount().interestRateProperty().get() * 100);
    }

    private String generateAnswer(String query, Map<String, Object> context) {
        // In a real implementation, this would use an LLM with the context
        // For now, we'll use a template-based approach

        StringBuilder response = new StringBuilder();

        // Client greeting
        String clientName = (String) context.get("client_name");
        response.append("Hello ").append(clientName).append(". ");

        // Process specific query types
        if (context.containsKey("checking_balance") && query.contains("balance")) {
            generateBalanceResponse(response, context);
        }

        if (context.containsKey("total_transactions") && containsAny(query, "spend", "transaction", "history")) {
            generateTransactionResponse(response, context);
        }

        if (context.containsKey("credit_limit") && containsAny(query, "credit", "card")) {
            generateCreditResponse(response, context);
        }

        if (context.containsKey("withdrawal_limit") && containsAny(query, "savings", "save", "interest")) {
            generateSavingsResponse(response, context);
        }

        // If no specific responses were generated, provide a general response
        if (response.length() <= clientName.length() + 8) {
            response.append("I'm not sure I understand your question. You can ask me about your account balances, recent transactions, credit card information, or savings account details.");
        }

        return response.toString();
    }

    private void generateBalanceResponse(StringBuilder response, Map<String, Object> context) {
        double checkingBalance = (double) context.get("checking_balance");
        double savingsBalance = (double) context.get("savings_balance");

        response.append("Your checking account balance is $").append(String.format("%.2f", checkingBalance))
                .append(" and your savings account balance is $").append(String.format("%.2f", savingsBalance))
                .append(". ");

        if (context.containsKey("credit_balance")) {
            double creditBalance = (double) context.get("credit_balance");
            double availableCredit = (double) context.get("available_credit");

            response.append("Your credit card balance is $").append(String.format("%.2f", creditBalance))
                    .append(" with $").append(String.format("%.2f", availableCredit))
                    .append(" available credit. ");
        }
    }

    private void generateTransactionResponse(StringBuilder response, Map<String, Object> context) {
        int days = (int) context.get("days_period");
        int totalTransactions = (int) context.get("total_transactions");
        double totalSpent = (double) context.get("total_spent");
        double totalReceived = (double) context.get("total_received");
        double netChange = (double) context.get("net_change");

        response.append("In the past ").append(days).append(" days, you've had ")
                .append(totalTransactions).append(" transactions. ")
                .append("You've spent $").append(String.format("%.2f", totalSpent))
                .append(" and received $").append(String.format("%.2f", totalReceived))
                .append(", for a net ").append(netChange >= 0 ? "increase" : "decrease")
                .append(" of $").append(String.format("%.2f", Math.abs(netChange)))
                .append(". ");

        // Add details about the most recent transactions if available
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recentTransactions = (List<Map<String, Object>>) context.get("recent_transactions");
        if (recentTransactions != null && !recentTransactions.isEmpty()) {
            response.append("Your most recent transaction was on ")
                    .append(recentTransactions.get(0).get("date"))
                    .append(" for $").append(String.format("%.2f", (double) recentTransactions.get(0).get("amount")))
                    .append(". ");
        }
    }

    private void generateCreditResponse(StringBuilder response, Map<String, Object> context) {
        double creditBalance = (double) context.get("credit_balance");
        double creditLimit = (double) context.get("credit_limit");
        double availableCredit = (double) context.get("available_credit");
        double minimumPayment = (double) context.get("minimum_payment");
        String paymentDueDate = (String) context.get("payment_due_date");
        double apr = (double) context.get("apr");

        response.append("Your credit card has a balance of $").append(String.format("%.2f", creditBalance))
                .append(" with a credit limit of $").append(String.format("%.2f", creditLimit))
                .append(". Your available credit is $").append(String.format("%.2f", availableCredit))
                .append(". Your minimum payment of $").append(String.format("%.2f", minimumPayment))
                .append(" is due on ").append(paymentDueDate)
                .append(". Your current APR is ").append(String.format("%.2f", apr))
                .append("%. ");
    }

    private void generateSavingsResponse(StringBuilder response, Map<String, Object> context) {
        double savingsBalance = (double) context.get("savings_balance");
        double withdrawalLimit = (double) context.get("withdrawal_limit");
        double interestRate = (double) context.get("interest_rate");

        response.append("Your savings account balance is $").append(String.format("%.2f", savingsBalance))
                .append(". You can withdraw up to $").append(String.format("%.2f", withdrawalLimit))
                .append(" per transaction. Your savings account earns interest at a rate of ")
                .append(String.format("%.2f", interestRate)).append("% per year. ");
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }
}