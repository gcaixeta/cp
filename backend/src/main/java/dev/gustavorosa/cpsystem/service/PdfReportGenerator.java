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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
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
            "Janeiro", "Fevereiro", "Marco", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    public byte[] generate(MonthlyReportData data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 50);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new FooterPageEvent());
            document.open();

            addHeader(document, data);
            addClientInfo(document, data.client());
            addSummaryCards(document, data);
            addGroupBreakdownTable(document, data);
            addPaymentsTable(document, data);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF report", e);
            throw new ReportGenerationException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    private void addHeader(Document document, MonthlyReportData data) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 2});

        // Logo
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(PdfPCell.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            ClassPathResource logoResource = new ClassPathResource("static/logo.png");
            if (logoResource.exists()) {
                Image logo = Image.getInstance(logoResource.getURL());
                logo.scaleToFit(80, 80);
                logoCell.addElement(logo);
            }
        } catch (Exception e) {
            log.warn("Could not load logo image, skipping: {}", e.getMessage());
        }
        headerTable.addCell(logoCell);

        // Title
        String monthName = MONTH_NAMES[data.month() - 1];
        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(PdfPCell.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph title = new Paragraph("Relatorio Mensal", TITLE_FONT);
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
        addInfoRow(infoTable, "Endereco", client.getAddress());

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

        PdfPTable cards = new PdfPTable(3);
        cards.setWidthPercentage(100);
        cards.setSpacingBefore(8);
        cards.setWidths(new float[]{1, 1, 1});

        addCard(cards, "Pagos em Dia", data.paidOnTime() + " (" + String.format("%.0f%%", data.paidOnTimePercentage()) + ")", new Color(22, 163, 74));
        addCard(cards, "Pagos com Atraso", data.paidLate() + " (" + String.format("%.0f%%", data.paidLatePercentage()) + ")", new Color(234, 179, 8));
        addCard(cards, "Pendentes / Vencidos", data.pending() + " / " + data.overdue(), new Color(239, 68, 68));

        document.add(cards);

        PdfPTable financialCards = new PdfPTable(3);
        financialCards.setWidthPercentage(100);
        financialCards.setSpacingBefore(8);
        financialCards.setWidths(new float[]{1, 1, 1});

        addCard(financialCards, "Valor Recebido", CURRENCY_FMT.format(data.totalReceived()), new Color(22, 163, 74));
        addCard(financialCards, "Valor em Aberto", CURRENCY_FMT.format(data.totalOutstanding()), new Color(239, 68, 68));
        String avgDays = data.averageDaysDifference() >= 0
                ? String.format("%.1f dias de antecedencia", data.averageDaysDifference())
                : String.format("%.1f dias de atraso", Math.abs(data.averageDaysDifference()));
        addCard(financialCards, "Media de Pagamento", avgDays, PRIMARY_COLOR);

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

    private void addGroupBreakdownTable(Document document, MonthlyReportData data) throws DocumentException {
        if (data.groupBreakdowns().isEmpty()) return;

        Paragraph sectionTitle = new Paragraph("Detalhamento por Contrato", SUBTITLE_FONT);
        sectionTitle.setSpacingBefore(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        table.setWidths(new float[]{2.5f, 1, 1, 1, 1, 1.5f, 1.5f});

        addTableHeader(table, "Contrato", "Total", "Em Dia", "Atrasado", "Pend/Venc", "Recebido", "Em Aberto");

        boolean alternate = false;
        for (MonthlyReportData.GroupBreakdown gb : data.groupBreakdowns()) {
            Color bgColor = alternate ? LIGHT_BG : Color.WHITE;
            addTableCell(table, gb.groupName() != null ? gb.groupName() : "Contrato #" + gb.groupId(), bgColor);
            addTableCell(table, String.valueOf(gb.totalPayments()), bgColor);
            addTableCell(table, String.valueOf(gb.paidOnTime()), bgColor);
            addTableCell(table, String.valueOf(gb.paidLate()), bgColor);
            addTableCell(table, gb.pending() + "/" + gb.overdue(), bgColor);
            addTableCell(table, CURRENCY_FMT.format(gb.received()), bgColor);
            addTableCell(table, CURRENCY_FMT.format(gb.outstanding()), bgColor);
            alternate = !alternate;
        }

        document.add(table);
        addSeparator(document);
    }

    private void addPaymentsTable(Document document, MonthlyReportData data) throws DocumentException {
        Paragraph sectionTitle = new Paragraph("Pagamentos do Mes", SUBTITLE_FONT);
        sectionTitle.setSpacingBefore(10);
        document.add(sectionTitle);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setSpacingBefore(8);
        table.setWidths(new float[]{2.5f, 1, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f});

        addTableHeader(table, "Contrato", "Parcela", "Vencimento", "Pagamento", "Valor", "Valor Corrigido", "Status");

        boolean alternate = false;
        for (Payment p : data.allPayments()) {
            Color bgColor = alternate ? LIGHT_BG : Color.WHITE;
            String groupName = p.getPaymentGroup().getGroupName() != null
                    ? p.getPaymentGroup().getGroupName()
                    : "Contrato #" + p.getPaymentGroup().getId();
            addTableCell(table, groupName, bgColor);
            addTableCell(table, p.getInstallmentNumber() + "/" + p.getTotalInstallments(), bgColor);
            addTableCell(table, p.getDueDate().format(DATE_FMT), bgColor);
            addTableCell(table, p.getPaymentDate() != null ? p.getPaymentDate().format(DATE_FMT) : "-", bgColor);
            addTableCell(table, CURRENCY_FMT.format(p.getOriginalValue()), bgColor);
            addTableCell(table, p.getOverdueValue() != null ? CURRENCY_FMT.format(p.getOverdueValue()) : "-", bgColor);
            addTableCell(table, translateStatus(p.getPaymentStatus()), bgColor);
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

    private String translateStatus(PaymentStatus status) {
        return switch (status) {
            case PAID -> "Pago";
            case PAID_LATE -> "Pago c/ Atraso";
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

                PdfPCell pageCell = new PdfPCell(new Phrase("Pagina " + writer.getPageNumber(), FOOTER_FONT));
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
