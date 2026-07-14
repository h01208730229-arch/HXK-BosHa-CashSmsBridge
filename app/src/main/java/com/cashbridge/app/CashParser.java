package com.cashbridge.app;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses Vodafone Cash incoming-transfer SMS messages only. */
public final class CashParser {
    private static final Pattern AMOUNT = Pattern.compile(
            "تم\\s+استلام\\s+مبلغ\\s+([0-9٠-٩]+(?:[.,][0-9٠-٩]{1,2})?)\\s*جنيه",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern SENDER_PHONE = Pattern.compile(
            "من\\s+رقم\\s+((?:\\+?20|0020)?01[0125][0-9٠-٩]{8})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern RECEIVER_PHONE = Pattern.compile(
            "على\\s+رقم\\s+محفظتك\\s+((?:\\+?20|0020)?01[0125][0-9٠-٩]{8})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern TX = Pattern.compile(
            "رقم\\s+العملية\\s*[:：]?\\s*([0-9٠-٩]{5,30})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private CashParser() {}

    public static CashMessage parse(String senderId, String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;

        String text = normalizeDigits(raw);
        String lower = text.toLowerCase(Locale.ROOT);

        // Parse receipt-like incoming notifications even if the SMS sender is suspicious.
        // The server performs the authoritative VF-Cash sender check and moves
        // suspicious messages to manual review without crediting any balance.
        if (!lower.contains("تم استلام مبلغ")) return null;

        // Explicitly reject outgoing/withdrawal-style notifications.
        if (lower.contains("تم تحويل مبلغ") || lower.contains("تم سحب") || lower.contains("تم دفع")) {
            return null;
        }

        Matcher amountMatcher = AMOUNT.matcher(text);
        Matcher senderMatcher = SENDER_PHONE.matcher(text);
        Matcher receiverMatcher = RECEIVER_PHONE.matcher(text);
        Matcher txMatcher = TX.matcher(text);

        if (!amountMatcher.find() || !senderMatcher.find() || !receiverMatcher.find() || !txMatcher.find()) {
            return null;
        }

        CashMessage message = new CashMessage();
        message.provider = "vodafone_cash";
        message.senderId = senderId == null ? "" : senderId;
        message.rawText = raw;
        message.amount = Double.parseDouble(amountMatcher.group(1).replace(',', '.'));
        message.senderPhone = normalizePhone(senderMatcher.group(1));
        message.receiverPhone = normalizePhone(receiverMatcher.group(1));
        message.transactionId = normalizeDigits(txMatcher.group(1));

        if (message.senderPhone == null || message.receiverPhone == null || message.transactionId.isEmpty()) {
            return null;
        }

        return message;
    }

    private static String normalizePhone(String value) {
        if (value == null) return null;
        String digits = normalizeDigits(value).replaceAll("\\D+", "");
        if (digits.startsWith("0020")) digits = digits.substring(4);
        else if (digits.startsWith("20") && digits.length() == 12) digits = digits.substring(2);
        if (digits.length() == 10 && digits.startsWith("1")) digits = "0" + digits;
        return digits.matches("^01[0125][0-9]{8}$") ? digits : null;
    }

    private static String normalizeDigits(String value) {
        return value.replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4')
                .replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9');
    }
}
