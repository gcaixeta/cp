package dev.gustavorosa.cpsystem.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import dev.gustavorosa.cpsystem.api.response.MonthlyReportData;
import dev.gustavorosa.cpsystem.exception.ReportGenerationException;
import dev.gustavorosa.cpsystem.model.Client;
import dev.gustavorosa.cpsystem.model.Payment;
import dev.gustavorosa.cpsystem.model.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@Slf4j
public class PdfReportGenerator {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FMT = NumberFormat.getCurrencyInstance(PT_BR);

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(33, 37, 41));
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(33, 37, 41));
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(33, 37, 41));
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(33, 37, 41));
    private static final Font FOOTER_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));

    private static final Color PRIMARY_COLOR = new Color(37, 99, 235);
    private static final Color LIGHT_BG = new Color(248, 249, 250);
    private static final Color BORDER_COLOR = new Color(222, 226, 230);

    private static final String[] MONTH_NAMES = {
            "Janeiro", "Fevereiro", "Mar\u00e7o", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    public byte[] generate(MonthlyReportData data) {
        System.setProperty("java.awt.headless", "true");
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 50);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new FooterPageEvent());
            document.open();

            addHeader(document, data);
            addClientInfo(document, data.client());
            addSummaryCards(document, data);
            addCharts(document, data);
            addPaymentsTable(document, data);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF report", e);
            throw new ReportGenerationException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    private void addHeader(Document document, MonthlyReportData data) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 2, 2});

        // Logo
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(PdfPCell.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            ClassPathResource logoResource = new ClassPathResource("static/logo.JPG");
            if (logoResource.exists()) {
                Image logo = Image.getInstance(logoResource.getURL());
                logo.scaleToFit(80, 80);
                logoCell.addElement(logo);
            }
        } catch (Exception e) {
            log.warn("Could not load logo image, skipping: {}", e.getMessage());
        }
        headerTable.addCell(logoCell);

        // Client name
        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(PdfPCell.NO_BORDER);
        clientCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph clientName = new Paragraph(data.client().getName(), SUBTITLE_FONT);
        clientName.setAlignment(Element.ALIGN_LEFT);
        clientCell.addElement(clientName);
        headerTable.addCell(clientCell);

        // Title
        String monthName = MONTH_NAMES[data.month() - 1];
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(PdfPCell.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph title = new Paragraph("Relat\u00f3rio Mensal", TITLE_FONT);
        title.setAlignment(Element.ALIGN_RIGHT);
        Paragraph period = new Paragraph(monthName + " / " + data.year(), SUBTITLE_FONT);
        period.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(title);
        titleCell.addElement(period);
        headerTable.addCell(titleCell);

        document.add(headerTable);
        addSeparator(document);
    }

    private void addClientInfo(Document document, Client client) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Dados do Cliente", SUBTITLE_FONT);
        sectionTitle.setSpacingBefore(10);
        document.add(sectionTitle);

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingBefore(5);
        infoTable.setWidths(new float[]{1, 1});

        addInfoRow(infoTable, "Nome", client.getName());
        addInfoRow(infoTable, "CPF/CNPJ", formatDocument(client.getDocument()));
        addInfoRow(infoTable, "Telefone", client.getPhone() != null ? client.getPhone() : "-");
        addInfoRow(infoTable, "Endere\u00e7o", client.getAddress());

        document.add(infoTable);
        addSeparator(document);
    }

    private void addInfoRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label + ":", LABEL_FONT));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPaddingBottom(4);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, BODY_FONT));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPaddingBottom(4);
        table.addCell(valueCell);
    }

    private void addSummaryCards(Document document, MonthlyReportData data) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Resumo", SUBTITLE_FONT);
        sectionTitle.setSpacingBefore(10);
        document.add(sectionTitle);

        PdfPTable cards = new PdfPTable(4);
        cards.setWidthPercentage(100);
        cards.setSpacingBefore(8);
        cards.setWidths(new float[]{1, 1, 1, 1});

        addCard(cards, "Antecipado",
                data.paidEarly() + " (" + String.format("%.0f%%", data.paidEarlyPercentage()) + ")",
                new Color(22, 163, 74));
        addCard(cards, "Na Data",
                data.paidOnDueDate() + " (" + String.format("%.0f%%", data.paidOnDueDatePercentage()) + ")",
                new Color(59, 130, 246));
        addCard(cards, "Atrasado",
                data.paidLate() + " (" + String.format("%.0f%%", data.paidLatePercentage()) + ")",
                new Color(234, 179, 8));
        addCard(cards, "Pendentes / Vencidos",
                data.pending() + " / " + data.overdue(),
                new Color(239, 68, 68));

        document.add(cards);

        PdfPTable financialCards = new PdfPTable(3);
        financialCards.setWidthPercentage(100);
        financialCards.setSpacingBefore(8);
        financialCards.setWidths(new float[]{1, 1, 1});

        addCard(financialCards, "Valor Recebido", CURRENCY_FMT.format(data.totalReceived()), new Color(22, 163, 74));
        addCard(financialCards, "Valor em Aberto", CURRENCY_FMT.format(data.totalOutstanding()), new Color(239, 68, 68));

        String avgDaysText = data.averageDaysLate() > 0
                ? String.format("%.1f dias", data.averageDaysLate())
                : "N/A";
        addCard(financialCards, "M\u00e9dia de Atraso", avgDaysText, PRIMARY_COLOR);

        document.add(financialCards);
        addSeparator(document);
    }

    private void addCard(PdfPTable table, String label, String value, Color accentColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER_COLOR);
        cell.setBorderWidth(1);
        cell.setPadding(10);
        cell.setBackgroundColor(LIGHT_BG);

        Paragraph labelP = new Paragraph(label, LABEL_FONT);
        cell.addElement(labelP);

        Font valueFont = new Font(Font.HELVETICA, 13, Font.BOLD, accentColor);
        Paragraph valueP = new Paragraph(value, valueFont);
        valueP.setSpacingBefore(4);
        cell.addElement(valueP);

        table.addCell(cell);
    }

    private void addCharts(Document document, MonthlyReportData data) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Gr\u00e1ficos", SUBTITLE_FONT);
        sectionTitle.setSpacingBefore(10);
        document.add(sectionTitle);

        try {
            // Pie chart — payment status distribution
            DefaultPieDataset<String> pieDataset = new DefaultPieDataset<>();
            if (data.paidEarly() > 0)     pieDataset.setValue("Antecipado", data.paidEarly());
            if (data.paidOnDueDate() > 0) pieDataset.setValue("Na Data", data.paidOnDueDate());
            if (data.paidLate() > 0)      pieDataset.setValue("Atrasado", data.paidLate());
            if (data.pending() > 0)       pieDataset.setValue("Pendente", data.pending());
            if (data.overdue() > 0)       pieDataset.setValue("Vencido", data.overdue());

            JFreeChart pieChart = ChartFactory.createPieChart(
                    "Distribui\u00e7\u00e3o de Pagamentos", pieDataset, true, false, false);
            pieChart.setBackgroundPaint(java.awt.Color.WHITE);
            PiePlot<?> piePlot = (PiePlot<?>) pieChart.getPlot();
            piePlot.setBackgroundPaint(java.awt.Color.WHITE);
            piePlot.setSectionPaint("Antecipado",  new java.awt.Color(22, 163, 74));
            piePlot.setSectionPaint("Na Data",     new java.awt.Color(59, 130, 246));
            piePlot.setSectionPaint("Atrasado",    new java.awt.Color(234, 179, 8));
            piePlot.setSectionPaint("Pendente",    new java.awt.Color(249, 115, 22));
            piePlot.setSectionPaint("Vencido",     new java.awt.Color(239, 68, 68));
            pieChart.setTitle((org.jfree.chart.title.TextTitle) null);
            piePlot.setOutlineVisible(false);
            piePlot.setShadowPaint(null);
            piePlot.setLabelBackgroundPaint(java.awt.Color.WHITE);
            piePlot.setLabelShadowPaint(null);
            piePlot.setLabelOutlinePaint(null);
            piePlot.setLabelFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 11));
            piePlot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}\n{2}"));
            piePlot.setInteriorGap(0.05);
            piePlot.setLabelGap(0.02);
            if (pieChart.getLegend() != null) {
                pieChart.getLegend().setFrame(new BlockBorder(java.awt.Color.WHITE));
            }

            // Bar chart — received vs outstanding
            DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
            barDataset.addValue(data.totalReceived(), "Recebido", "Valores");
            barDataset.addValue(data.totalOutstanding(), "Em Aberto", "Valores");

            JFreeChart barChart = ChartFactory.createBarChart(
                    "Recebido vs Em Aberto", null, "R$",
                    barDataset, PlotOrientation.HORIZONTAL, true, false, false);
            barChart.setBackgroundPaint(java.awt.Color.WHITE);
            CategoryPlot barPlot = barChart.getCategoryPlot();
            barPlot.setBackgroundPaint(java.awt.Color.WHITE);
            BarRenderer renderer = (BarRenderer) barPlot.getRenderer();
            renderer.setSeriesPaint(0, new java.awt.Color(22, 163, 74));
            renderer.setSeriesPaint(1, new java.awt.Color(239, 68, 68));
            barChart.setTitle((org.jfree.chart.title.TextTitle) null);
            renderer.setDrawBarOutline(false);
            renderer.setShadowVisible(false);
            renderer.setMaximumBarWidth(0.4);
            barPlot.setOutlineVisible(false);
            barPlot.setRangeGridlinesVisible(true);
            barPlot.setRangeGridlinePaint(new java.awt.Color(220, 220, 220));
            barPlot.getDomainAxis().setVisible(false);
            java.awt.Font axisFont = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10);
            barPlot.getRangeAxis().setLabelFont(axisFont);
            barPlot.getRangeAxis().setTickLabelFont(axisFont);
            NumberAxis rangeAxis = (NumberAxis) barPlot.getRangeAxis();
            rangeAxis.setNumberFormatOverride(NumberFormat.getCurrencyInstance(PT_BR));
            if (barChart.getLegend() != null) {
                barChart.getLegend().setFrame(new BlockBorder(java.awt.Color.WHITE));
            }

            // Embed charts as PNG images in PDF
            Image pdfPieImage = chartToImage(pieChart, 600, 440);
            Image pdfBarImage = chartToImage(barChart, 600, 440);
            pdfPieImage.scaleToFit(260, 195);
            pdfBarImage.scaleToFit(260, 195);

            PdfPTable chartTable = new PdfPTable(2);
            chartTable.setWidthPercentage(100);
            chartTable.setSpacingBefore(8);
            chartTable.setWidths(new float[]{1, 1});

            PdfPCell pieCell = new PdfPCell(pdfPieImage, true);
            pieCell.setBorder(PdfPCell.NO_BORDER);
            pieCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            chartTable.addCell(pieCell);

            PdfPCell barCell = new PdfPCell(pdfBarImage, true);
            barCell.setBorder(PdfPCell.NO_BORDER);
            barCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            chartTable.addCell(barCell);

            document.add(chartTable);
        } catch (Exception e) {
            log.warn("Could not render charts, skipping: {}", e.getMessage());
        }

        addSeparator(document);
    }

    private Image chartToImage(JFreeChart chart, int width, int height) throws Exception {
        BufferedImage bufferedImage = chart.createBufferedImage(width, height);
        ByteArrayOutputStream imgOs = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "PNG", imgOs);
        return Image.getInstance(imgOs.toByteArray());
    }

    private void addPaymentsTable(Document document, MonthlyReportData data) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Pagamentos do M\u00eas", SUBTITLE_FONT);
        sectionTitle.setSpacingBefore(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        table.setWidths(new float[]{2.5f, 1, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f});

        addTableHeader(table, "Pagador", "Parcela", "Vencimento", "Pagamento", "Valor", "Valor Corrigido", "Status");

        boolean alternate = false;
        for (Payment p : data.allPayments()) {
            Color bgColor = alternate ? LIGHT_BG : Color.WHITE;
            addTableCell(table, p.getPayerName(), bgColor);
            addTableCell(table, p.getInstallmentNumber() + "/" + p.getTotalInstallments(), bgColor);
            addTableCell(table, p.getDueDate().format(DATE_FMT), bgColor);
            addTableCell(table, p.getPaymentDate() != null ? p.getPaymentDate().format(DATE_FMT) : "-", bgColor);
            addTableCell(table, CURRENCY_FMT.format(p.getOriginalValue()), bgColor);
            addTableCell(table, p.getOverdueValue() != null ? CURRENCY_FMT.format(p.getOverdueValue()) : "-", bgColor);
            addTableCell(table, translatePaymentStatus(p), bgColor);
            alternate = !alternate;
        }

        document.add(table);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setBackgroundColor(PRIMARY_COLOR);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBorderColor(PRIMARY_COLOR);
            table.addCell(cell);
        }
    }

    private void addTableCell(PdfPTable table, String text, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY_FONT));
        cell.setPadding(5);
        cell.setBackgroundColor(bgColor);
        cell.setBorderColor(BORDER_COLOR);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addSeparator(Document document) throws DocumentException {
        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100);
        separator.setSpacingBefore(8);
        separator.setSpacingAfter(4);
        PdfPCell cell = new PdfPCell();
        cell.setBorderWidthBottom(1);
        cell.setBorderColorBottom(BORDER_COLOR);
        cell.setBorderWidthTop(0);
        cell.setBorderWidthLeft(0);
        cell.setBorderWidthRight(0);
        cell.setFixedHeight(1);
        separator.addCell(cell);
        document.add(separator);
    }

    private String translatePaymentStatus(Payment p) {
        return switch (p.getPaymentStatus()) {
            case PAID -> (p.getPaymentDate() != null && p.getPaymentDate().isBefore(p.getDueDate()))
                    ? "Antecipado" : "Na Data";
            case PAID_LATE -> "Atrasado";
            case PENDING -> "Pendente";
            case OVERDUE -> "Vencido";
            case CANCELED -> "Cancelado";
        };
    }

    private String formatDocument(String doc) {
        if (doc == null) return "-";
        if (doc.length() == 11) {
            return doc.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
        } else if (doc.length() == 14) {
            return doc.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
        }
        return doc;
    }

    private static class FooterPageEvent extends com.lowagie.text.pdf.PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfPTable footer = new PdfPTable(2);
            try {
                footer.setWidthPercentage(100);
                footer.setWidths(new float[]{1, 1});
                footer.setTotalWidth(document.right() - document.left());

                PdfPCell dateCell = new PdfPCell(new Phrase("Gerado em " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), FOOTER_FONT));
                dateCell.setBorder(PdfPCell.TOP);
                dateCell.setBorderColorTop(BORDER_COLOR);
                dateCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                dateCell.setPaddingTop(5);
                footer.addCell(dateCell);

                PdfPCell pageCell = new PdfPCell(new Phrase("P\u00e1gina " + writer.getPageNumber(), FOOTER_FONT));
                pageCell.setBorder(PdfPCell.TOP);
                pageCell.setBorderColorTop(BORDER_COLOR);
                pageCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                pageCell.setPaddingTop(5);
                footer.addCell(pageCell);

                footer.writeSelectedRows(0, -1, document.left(), document.bottom() - 5, writer.getDirectContent());
            } catch (DocumentException e) {
                // ignore footer errors
            }
        }
    }
}
