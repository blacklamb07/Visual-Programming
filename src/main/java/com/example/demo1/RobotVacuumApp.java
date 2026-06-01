package com.example.demo1;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RobotVacuumApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        VacuumModel model = new VacuumModel();
        RobotVacuumView view = new RobotVacuumView();
        new RobotVacuumController(model, view);

        // SAĞ TARAF İÇİN YENİ VE DAHA DENGELİ YERLEŞİM (VBox)
        VBox rightSide = new VBox();
        rightSide.setStyle("-fx-background-color: #0F172A;");
        HBox.setHgrow(rightSide, Priority.ALWAYS); // Sağ panel kalan yatay alanı kaplasın

        // Haritayı (Canvas) merkeze yerleştir
        StackPane centerPane = new StackPane(view.getCanvas());
        centerPane.setAlignment(Pos.CENTER);
        // EN ÖNEMLİ KISIM: Harita dikeydeki tüm boşluğu esnekçe kullansın, böylece alt panel ekran dışına itilmez
        VBox.setVgrow(centerPane, Priority.ALWAYS);

        // Alt Panel (Kenar boşluklarıyla VBox'a ekliyoruz)
        HBox bottomPanel = view.getBottomPanel();
        VBox.setMargin(bottomPanel, new Insets(0, 20, 20, 20));

        // Sağ tarafı birleştir (Üstte başlık, ortada esnek harita, altta sabit panel)
        rightSide.getChildren().addAll(view.getHeaderPanel(), centerPane, bottomPanel);

        // ANA PENCERE (Sol Menü + Sağ Taraf)
        HBox root = new HBox();
        root.getChildren().addAll(view.getLeftPanel(), rightSide);
        root.setStyle("-fx-background-color: #0F172A;");

        Scene scene = new Scene(root, 1220, 780);
        primaryStage.setTitle("Robot Süpürge Simülasyonu - BZ214 Visual Programming");
        primaryStage.setScene(scene);

        primaryStage.setResizable(true);
        primaryStage.setMaximized(true); // Tam ekran (görev çubuğu dahil)

        primaryStage.show();

        // Başlangıç çizimi
        view.draw(model);
        view.updateStats(model);
    }

    public static void main(String[] args) {
        launch(args);
    }
}