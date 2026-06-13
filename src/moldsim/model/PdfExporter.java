package moldsim.model;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;
import java.util.List;

/**
 * Utility class responsible for generating a PDF report from simulation
 * statistics, environment data, and alert logs.
 * <p>
 * The report includes environment configuration, weekly statistics,
 * final simulation state, and historical alert logs.
 */
public class PdfExporter {

    /** Font used for the main title. */
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 30, Font.BOLD);
    /** Font used for section titles. */
    private static final Font SECTION_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    /** Font used for subsection headers. */
    private static final Font SUBSECTION_FONT = new Font(Font.FontFamily.HELVETICA,15);
    /** Font used for normal text content. */
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 11);
    /** Font used for footer text. */
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8);

    /**
     * Generates a complete PDF report for a simulation run.
     *
     * @param stats weekly simulation statistics
     * @param env simulation environment configuration
     * @param allLogs alert logs grouped by week
     * @param filePath output PDF file path
     */
    public static void export(List<Statistics> stats, Environment env, List<List<String>> allLogs, String filePath) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            Paragraph title = new Paragraph("MoldSim — Simulation Report", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            document.add(new Paragraph("Environment", SECTION_FONT));
            document.add(new Paragraph("Humidity    : " + env.getHumidity() + " %", NORMAL_FONT));
            document.add(new Paragraph("Temperature : " + env.getTemperature() + " °C", NORMAL_FONT));
            document.add(new Paragraph("Ventilation : " + env.getVentilation() + " %", NORMAL_FONT));
            document.add(Chunk.NEWLINE);

            document.add(new Paragraph("Week-by-week Statistics", SECTION_FONT));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setSpacingBefore(5);

            String[] headers = { "Week", "Healthy", "Infected", "Dead", "Trend", "Avg Age", "Avg Mold" };
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, SUBSECTION_FONT));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (int i = 0; i < stats.size(); i++) {
                Statistics s = stats.get(i);
                int total = s.getTotalCells();
                table.addCell(centeredCell(String.valueOf(i + 1)));
                table.addCell(centeredCell(s.getHealthyCells() + " (" + String.format("%.1f", 100.0 * s.getHealthyCells() / total) + "%)"));
                table.addCell(centeredCell(s.getInfectedCells() + " (" + String.format("%.1f", 100.0 * s.getInfectedCells() / total) + "%)"));
                table.addCell(centeredCell(s.getDeadCells() + " (" + String.format("%.1f", 100.0 * s.getDeadCells() / total) + "%)"));
                table.addCell(centeredCell(s.getTrend()));
                table.addCell(centeredCell(String.format("%.1f", s.getAverageAge())));
                table.addCell(centeredCell(String.format("%.1f", s.getAverageMoldLevel())));
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            if (!stats.isEmpty()) {
                Statistics last = stats.get(stats.size() - 1);
                document.add(new Paragraph("Final State", SECTION_FONT));
                document.add(new Paragraph("After " + stats.size() + ((stats.size() > 1) ? " weeks, " : " week, ") + last.getInfectedCells() + ((last.getInfectedCells() == 1) ? " cell is infected (" : " cells are infected (") + String.format("%.1f", 100.0 * last.getInfectedCells() / last.getTotalCells()) + "% of the wall surface).", NORMAL_FONT));

                double pct = 100.0 * last.getInfectedCells() / last.getTotalCells();
                String risk = pct < 5 ? "Low" : pct < 20 ? "Moderate" : pct < 40 ? "High" : "Critical";
                Paragraph riskPara = new Paragraph("Risk level : " + risk, SECTION_FONT);
                riskPara.setSpacingBefore(8);
                document.add(riskPara);
            }
            
            if (allLogs != null && !allLogs.isEmpty()) {
                document.add(Chunk.NEWLINE);
                document.add(new Paragraph("Alert History", SECTION_FONT));
                for (int i = 0; i < allLogs.size(); i++) {
                    List<String> logs = allLogs.get(i);
                    if (logs == null || logs.isEmpty()) continue;
                    document.add(new Paragraph("Week " + (i + 1), SUBSECTION_FONT));
                    for (String log : logs) {
                        document.add(new Paragraph("  • " + log, NORMAL_FONT));
                    }
                }
            }
            document.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph("Generated by MoldSim — This report is an estimation tool, not a certified diagnosis.", SMALL_FONT);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            System.out.println("PDF exported to: " + filePath);

        } catch (Exception e) {
            System.err.println("PDF export failed: " + e.getMessage());
        }
    }

    /**
     * Creates a centered PDF table cell with standard formatting.
     *
     * @param text cell content
     * @return formatted PDF cell
     */
    private static PdfPCell centeredCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(4);
        return cell;
    }
}