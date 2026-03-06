package com.plainquery.service;

import com.plainquery.model.QueryResult;

import javafx.embed.swing.SwingNode;
import javafx.scene.Node;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.awt.*;

public final class ChartServiceImpl implements ChartService {

    private static final Logger LOG = Logger.getLogger(ChartServiceImpl.class.getName());

    private static final int MAX_CHART_ROWS = 500;
    private static final int MAX_PIE_CATEGORIES = 10;

    public ChartServiceImpl() {}

    @Override
    public Optional<Node> buildChart(QueryResult result) {
        Objects.requireNonNull(result, "QueryResult must not be null");

        if (result.isEmpty() || result.getColumnNames().size() < 2) {
            return Optional.empty();
        }

        try {
            ChartType type = detectChartType(result);
            if (type == null) {
                return Optional.empty();
            }

            JFreeChart chart = buildJFreeChart(type, result);
            if (chart == null) {
                return Optional.empty();
            }

            // Customize chart aesthetics
            customizeChartAppearance(chart, type);

            SwingNode swingNode = new SwingNode();
            JFreeChart finalChart = chart;
            SwingUtilities.invokeLater(() -> swingNode.setContent(
                new org.jfree.chart.ChartPanel(finalChart)));

            return Optional.of(swingNode);

        } catch (Exception e) {
            LOG.warning("Chart generation failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    private ChartType detectChartType(QueryResult result) {
        List<String> cols = result.getColumnNames();
        if (cols.size() < 2) return null;

        String col0 = cols.get(0).toLowerCase();
        String col1 = cols.get(1).toLowerCase();

        boolean col0IsTime = col0.contains("date") || col0.contains("time")
            || col0.contains("year") || col0.contains("month") || col0.contains("day");
        boolean col1IsNumeric = isNumericColumn(result, 1);

        if (col0IsTime && col1IsNumeric) {
            return ChartType.LINE;
        }

        boolean col0IsCategorical = isCategoricalColumn(result, 0);
        if (col0IsCategorical && col1IsNumeric) {
            if (result.getRowCount() <= MAX_PIE_CATEGORIES) {
                return ChartType.PIE;
            }
            return ChartType.BAR;
        }

        // Check if all columns are numeric for scatter chart
        if (areAllColumnsNumeric(result)) {
            return ChartType.SCATTER;
        }

        return null;
    }

    private boolean areAllColumnsNumeric(QueryResult result) {
        List<String> cols = result.getColumnNames();
        for (int i = 0; i < cols.size(); i++) {
            if (!isNumericColumn(result, i)) {
                return false;
            }
        }
        return true;
    }

    private JFreeChart buildJFreeChart(ChartType type, QueryResult result) {
        List<List<Object>> rows = result.getRows();
        int limit = Math.min(rows.size(), MAX_CHART_ROWS);

        switch (type) {
            case BAR: {
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                for (int i = 0; i < limit; i++) {
                    List<Object> row = rows.get(i);
                    String category = objectToString(row.get(0));
                    double value    = objectToDouble(row.get(1));
                    dataset.addValue(value, "Value", category);
                }
                return ChartFactory.createBarChart(
                    "", result.getColumnNames().get(0),
                    result.getColumnNames().get(1),
                    dataset, PlotOrientation.VERTICAL, true, true, false);
            }
            case LINE: {
                XYSeries series = new XYSeries("Value");
                for (int i = 0; i < limit; i++) {
                    List<Object> row = rows.get(i);
                    double x = i;
                    double y = objectToDouble(row.get(1));
                    series.add(x, y);
                }
                XYSeriesCollection dataset = new XYSeriesCollection(series);
                return ChartFactory.createXYLineChart(
                    "", result.getColumnNames().get(0),
                    result.getColumnNames().get(1),
                    dataset, PlotOrientation.VERTICAL, true, true, false);
            }
            case PIE: {
                DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
                for (int i = 0; i < limit; i++) {
                    List<Object> row = rows.get(i);
                    String key   = objectToString(row.get(0));
                    double value = objectToDouble(row.get(1));
                    if (value > 0) {
                        dataset.setValue(key, value);
                    }
                }
                return ChartFactory.createPieChart(
                    "", dataset, true, true, false);
            }
            case SCATTER: {
                XYSeriesCollection dataset = new XYSeriesCollection();
                XYSeries series = new XYSeries("Data Points");
                for (int i = 0; i < limit; i++) {
                    List<Object> row = rows.get(i);
                    if (row.size() >= 2) {
                        double x = objectToDouble(row.get(0));
                        double y = objectToDouble(row.get(1));
                        series.add(x, y);
                    }
                }
                dataset.addSeries(series);
                return ChartFactory.createScatterPlot(
                    "", result.getColumnNames().get(0),
                    result.getColumnNames().get(1),
                    dataset, PlotOrientation.VERTICAL, true, true, false);
            }
            default:
                return null;
        }
    }

    private boolean isNumericColumn(QueryResult result, int colIndex) {
        for (List<Object> row : result.getRows()) {
            if (colIndex >= row.size()) continue;
            Object val = row.get(colIndex);
            if (val == null) continue;
            if (val instanceof Number) return true;
            try {
                Double.parseDouble(val.toString().replace(",", ""));
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private boolean isCategoricalColumn(QueryResult result, int colIndex) {
        if (isNumericColumn(result, colIndex)) return false;
        return true;
    }

    private String objectToString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private double objectToDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString().replace(",", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private enum ChartType {
        BAR, LINE, PIE, SCATTER
    }

    private void customizeChartAppearance(JFreeChart chart, ChartType type) {
        // Set background colors
        chart.setBackgroundPaint(new Color(43, 43, 43)); // Dark background
        chart.getPlot().setBackgroundPaint(new Color(50, 50, 50)); // Plot background

        // Customize axes
        if (chart.getPlot() instanceof CategoryPlot) {
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            plot.setDomainGridlinePaint(Color.GRAY);
            plot.setRangeGridlinePaint(Color.GRAY);

            // Customize bar renderer
            if (plot.getRenderer() instanceof BarRenderer) {
                BarRenderer renderer = (BarRenderer) plot.getRenderer();
                renderer.setSeriesPaint(0, new Color(74, 144, 217)); // Blue bars
                renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
                renderer.setDefaultItemLabelsVisible(true);
                renderer.setMaximumBarWidth(0.8);
            }

            // Customize axes
            if (plot.getDomainAxis() instanceof CategoryAxis) {
                CategoryAxis domainAxis = (CategoryAxis) plot.getDomainAxis();
                domainAxis.setLabelPaint(Color.WHITE);
                domainAxis.setTickLabelPaint(Color.LIGHT_GRAY);
            }
            if (plot.getRangeAxis() instanceof NumberAxis) {
                NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
                rangeAxis.setLabelPaint(Color.WHITE);
                rangeAxis.setTickLabelPaint(Color.LIGHT_GRAY);
                rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
            }
        } else if (chart.getPlot() instanceof XYPlot) {
            XYPlot plot = (XYPlot) chart.getPlot();
            plot.setDomainGridlinePaint(Color.GRAY);
            plot.setRangeGridlinePaint(Color.GRAY);

            // Customize line renderer
            if (plot.getRenderer() instanceof XYLineAndShapeRenderer) {
                XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
                renderer.setSeriesPaint(0, new Color(74, 144, 217)); // Blue line
                renderer.setSeriesStroke(0, new BasicStroke(2.0f));
                renderer.setDefaultShapesVisible(true);
                renderer.setDefaultShape(new java.awt.geom.Ellipse2D.Double(-3.0, -3.0, 6.0, 6.0));
            }

            // Customize axes
            if (plot.getDomainAxis() instanceof NumberAxis) {
                NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
                domainAxis.setLabelPaint(Color.WHITE);
                domainAxis.setTickLabelPaint(Color.LIGHT_GRAY);
                domainAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
            }
            if (plot.getRangeAxis() instanceof NumberAxis) {
                NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
                rangeAxis.setLabelPaint(Color.WHITE);
                rangeAxis.setTickLabelPaint(Color.LIGHT_GRAY);
                rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
            }
        } else if (chart.getPlot() instanceof PiePlot) {
            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setLabelBackgroundPaint(new Color(43, 43, 43));
            plot.setLabelPaint(Color.WHITE);
            plot.setLabelOutlinePaint(null);
            plot.setLabelShadowPaint(null);
            plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0} ({2})"));

            // Set different colors for pie slices
            Color[] colors = {
                new Color(74, 144, 217), // Blue
                new Color(231, 76, 60), // Red
                new Color(243, 156, 18), // Orange
                new Color(46, 204, 113), // Green
                new Color(155, 89, 182), // Purple
                new Color(52, 152, 219), // Light Blue
                new Color(230, 126, 34), // Dark Orange
                new Color(39, 174, 96), // Dark Green
                new Color(142, 68, 173), // Dark Purple
                new Color(41, 128, 185) // Medium Blue
            };
            for (int i = 0; i < colors.length; i++) {
                plot.setSectionPaint(i, colors[i % colors.length]);
            }
        }

        // Customize legend
        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(new Color(43, 43, 43));
            if (chart.getLegend() instanceof LegendTitle) {
                LegendTitle legend = (LegendTitle) chart.getLegend();
                legend.setItemPaint(Color.WHITE);
                legend.setBorder(0.0, 0.0, 0.0, 0.0);
            }
        }

        // Customize title
        if (chart.getTitle() != null) {
            chart.getTitle().setPaint(Color.WHITE);
            chart.getTitle().setFont(new Font("Arial", Font.BOLD, 14));
        }
    }
}
