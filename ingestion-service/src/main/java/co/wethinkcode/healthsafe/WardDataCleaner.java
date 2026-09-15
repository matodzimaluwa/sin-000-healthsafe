package co.wethinkcode.healthsafe;

public class WardDataCleaner {

    public WardRecord clean(String wardId,
                            String wing,
                            String department,
                            String bedsAvailable) {

        return new WardRecord(
                cleanWardId(wardId),
                cleanName(wing),
                cleanDepartment(department),
                cleanBeds(bedsAvailable)
        );
    }

    private String cleanWardId(String wardId) {
        if (wardId == null) {
            return null;
        }

        return wardId.trim().toUpperCase();
    }

    private String cleanName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String cleaned = value.trim().replaceAll("\\s+", " ").toLowerCase();

        return cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
    }

    private String cleanDepartment(String department) {
        if (department == null || department.isBlank()) {
            return null;
        }

        String cleaned = department.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();

        if (cleaned.equals("pediatrics")) {
            return "Paediatrics";
        }

        return cleaned.substring(0, 1).toUpperCase() + cleaned.substring(1);
    }

    private Integer cleanBeds(String bedsAvailable) {
        if (bedsAvailable == null || bedsAvailable.isBlank()) {
            return null;
        }

        try {
            int beds = Integer.parseInt(bedsAvailable.trim());

            if (beds < 0 || beds > 200) {
                return null;
            }

            return beds;

        } catch (NumberFormatException e) {
            return null;
        }
    }
}