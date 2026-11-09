
package com.muntinlupa.barangay_ease.ai;

public class ComplaintClassifyRequest {

    private String title;
    private String description;
    private String complaintText;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComplaintText() {
        return complaintText;
    }

    public void setComplaintText(String complaintText) {
        this.complaintText = complaintText;
    }

    public String getCombinedText() {

        StringBuilder text = new StringBuilder();

        if (title != null && !title.isBlank()) {

            text.append("Title: ")
                    .append(title.trim());
        }

        if (description != null && !description.isBlank()) {

            if (text.length() > 0) {
                text.append("\n");
            }

            text.append(description.trim());
        }

        // Legacy frontend fallback.
        if (text.length() == 0
                && complaintText != null
                && !complaintText.isBlank()) {

            text.append(complaintText.trim());
        }

        return text.toString().trim();
    }
}
