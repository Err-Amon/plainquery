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

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public final class ChartServiceImpl implements ChartService {

    private static final Logger LOG = Logger.getLogger(ChartServiceImpl.class.getName());

    private static final int MAX_CHART_ROWS = 500;

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
            if (result.getRowCount() <= 8) {
                return ChartType.PIE;
            }
            return ChartType.BAR;
        }

        return null;
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
                    dataset, PlotOrientation.VERTICAL, false, false, false);
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
                    dataset, PlotOrientation.VERTICAL, false, false, false);
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
                    "", dataset, false, false, false);
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
        BAR, LINE, PIE
    }
}
