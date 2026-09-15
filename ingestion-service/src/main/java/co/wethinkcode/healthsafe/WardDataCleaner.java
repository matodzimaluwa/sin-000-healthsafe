package co.wethinkcode.healthsafe;
import java.io.*;
import java.util.*;
import java.io.InputStreamReader;

public class WardDataCleaner {
    // Reads the CSV FILE AT THE GIVEN PATH AND RETURNS A CLEANED LIST OF WARD RECORD
    public static List<WardRecord> loadAndClean(String csvResourceName) throws IOException{
        List<WardRecord> rawRows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader((new InputStreamReader(
                WardDataCleaner.class.getClassLoader().getResourceAsStream(csvResourceName))))){
            String line = reader.readLine(); // Skips header row
            while((line = reader.readLine()) != null){
                if (line.isBlank()) continue;
                String[] parts = line.split(",", -1); // -1 keeps empty trailing fields
                if (parts.length < 4) continue;

                String wardId = clean(parts[0]);
                String wing = clean(parts[1]);
                String department = clean(parts[2]);
                BedsResult beds = parseBeds(parts[3]);

                rawRows.add(new WardRecord(
                        normaliseId(wardId),
                        normaliseWing(wing),
                        normaliseDept(department),
                        beds.value(),
                        beds.note()
                ));

            }

        }
        return mergeDuplicates(rawRows);
    }

    private static String clean(String value){
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normaliseId(String id){
        return id.toUpperCase();
    }

    private static String normaliseWing(String wing){
        if (wing.isEmpty()){
            return null; // missing wing
        }
        return capitalizeWords(wing);
    }

    private static String normaliseDept(String dept){
        String lower = dept.toLowerCase();
        if (lower.contains("paediatric") || lower.contains("pediatric")){
            return "Pediatrics";
        }
        if (lower.equals("icu")) {
            return "ICU";
        }
        return capitalizeWords(dept);
    }

    private static String capitalizeWords(String text){
        String[] words = text.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }


    private static final Set<String> PLACEHOLDER_BEDS =
            Set.of("n/a", "na", "tbd", "unknown", "-", "nan", "");
    // Bundles a parsed value with the reason it's null, so the caller never loses the "why".
    private record BedsResult(Integer value, String note) {}

    private static BedsResult parseBeds(String raw) {
        String cleaned = clean(raw);
        String lower = cleaned.toLowerCase();

        if (PLACEHOLDER_BEDS.contains(lower)) {
            return new BedsResult(null, "bedsAvailable was missing/placeholder ('" + cleaned + "')");
        }

        int num;
        try {
            num = Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return new BedsResult(null, "bedsAvailable was non-numeric ('" + cleaned + "') — flagged for follow-up");
        }

        if (num < 0) {
            return new BedsResult(null, "bedsAvailable was negative (" + num + ") — flagged for follow-up");
        }
        if (num > 100) {
            return new BedsResult(null, "bedsAvailable looked unrealistic (" + num + ") — flagged for follow-up");
        }
        return new BedsResult(num, null);
    }
    private static List<WardRecord> mergeDuplicates(List<WardRecord> rows){
        Map<String,WardRecord> merged = new LinkedHashMap<>();

        for (WardRecord row: rows){
            String key = row.wardId;
            if (!merged.containsKey(key)) {
                merged.put(key, row);
            } else {
                WardRecord existing = merged.get(key);
                WardRecord combined = new WardRecord(
                        key,
                        existing.wing != null ? existing.wing : row.wing,
                        existing.department != null ? existing.department : row.department,
                        existing.bedsAvailable != null ? existing.bedsAvailable : row.bedsAvailable,
                        "Merged duplicate rows for " + key
                );
                merged.put(key,combined);
            }
        }
        return new ArrayList<>(merged.values());
    }

}
