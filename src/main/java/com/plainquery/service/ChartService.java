package com.plainquery.service;

import com.plainquery.model.QueryResult;
import javafx.scene.Node;

import java.util.Optional;

public interface ChartService {

    Optional<Node> buildChart(QueryResult result);
}
