/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.tools.fx.monkey.tools;

import java.nio.charset.Charset;
import java.util.Base64;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import com.oracle.tools.fx.monkey.util.FX;

/**
 * CSS Playground Tool
 */
public class CssPlaygroundPane extends BorderPane {
    private final ColorPicker colorPicker;
    private final TextArea cssField;
    private static String oldCustom;
    private static String oldQuick;
    private int fontSize = 12;
    private static final int[] SIZES = {
        7,
        8,
        9,
        10,
        11,
        12,
        13,
        14,
        16,
        18,
        20,
        22
    };
    private final Label fontSizeLabel;

    public CssPlaygroundPane() {
        cssField = new TextArea();
        cssField.setId("CssPlaygroundPaneCss");

        colorPicker = new ColorPicker();

        Button fsLarger = FX.button("+", () -> fontSize(true));

        Button fsSmaller = FX.button("-", () -> fontSize(false));

        fontSizeLabel = new Label("12");
        fontSizeLabel.setAlignment(Pos.CENTER);

        BorderPane fs = new BorderPane(fontSizeLabel);
        fs.setLeft(fsSmaller);
        fs.setRight(fsLarger);

        Button updateButton = FX.button("Update", this::update);
        Button resetButton = FX.button("Reset", this::reset);

        GridPane p = new GridPane();
        p.setPadding(new Insets(10));
        p.setHgap(5);
        p.setVgap(5);
        int r = 0;
        p.add(new Label("Background Color:"), 0, r);
        p.add(colorPicker, 1, r);
        r++;
        p.add(new Label("Font Size:"), 0, r);
        p.add(fs, 1, r);

        Region spacer = new Region();
        HBox hb = new HBox(resetButton, spacer, updateButton);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        hb.setPadding(new Insets(5, 10, 5, 10));
        BorderPane cssPane = new BorderPane(cssField);
        cssPane.setBottom(hb);
        cssPane.setPadding(new Insets(2));

        TabPane tp = new TabPane();
        tp.getTabs().setAll(
            new Tab("Custom CSS", cssPane),
            new Tab("Quick", p)
            );

        setCenter(tp);

        colorPicker.setOnAction((ev) -> {
            updateQuick();
        });
    }

    private void fontSize(boolean larger) {
        fontSize = nextFontSize(larger);
        fontSizeLabel.setText(String.valueOf(fontSize));
        updateQuick();
    }

    private int nextFontSize(boolean larger) {
        int ix = indexOf(fontSize);
        if (ix < 0) {
            return 12;
        } else {
            ix += (larger ? 1 : -1);
            if (ix < 0) {
                ix = 0;
            } else if (ix >= SIZES.length) {
                ix = (SIZES.length - 1);
            }
            return SIZES[ix];
        }
    }

    private int indexOf(int val) {
        for (int i = 0; i < SIZES.length; i++) {
            if (SIZES[i] == val) {
                return i;
            }
        }
        return -1;
    }

    private void update() {
        String css = cssField.getText();
        applyStyleSheet(css, false);
    }

    private void updateQuick() {
        Color c = colorPicker.getValue();
        if (c == null) {
            c = Color.WHITE;
        }
        String css = generate(c);
        applyStyleSheet(css, true);
    }

    private String generate(Color bg) {
        StringBuilder sb = new StringBuilder();
        sb.append(".root {\n");

        sb.append(" -fx-base: " + toCssColor(bg) + ";\n");

        sb.append("-fx-font-size: ");
        sb.append(fontSize * 100.0 / 12.0);
        sb.append("%;\n");

        sb.append("}\n");

        return sb.toString();
    }

    private static String toCssColor(Color c) {
        int r = toInt8(c.getRed());
        int g = toInt8(c.getGreen());
        int b = toInt8(c.getBlue());
        return String.format("#%02X%02X%02X", r, g, b);
    }

    private static int toInt8(double x) {
        int v = (int)Math.round(x * 255);
        if (v < 0) {
            return 0;
        } else if (v > 255) {
            return 255;
        }
        return v;
    }

    private static String encode(String s) {
        if (s == null) {
            return null;
        }
        Charset utf8 = Charset.forName("utf-8");
        byte[] b = s.getBytes(utf8);
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(b);
    }

    private static void applyStyleSheet(String styleSheet, boolean quick) {
        String ss = encode(styleSheet);
        if (ss != null) {
            for (Window w: Window.getWindows()) {
                Scene scene = w.getScene();
                if (scene != null) {
                    ObservableList<String> sheets = scene.getStylesheets();
                    if (quick) {
                        if (oldQuick != null) {
                            sheets.remove(oldQuick);
                        }
                    } else {
                        if (oldCustom != null) {
                            sheets.remove(oldCustom);
                        }
                    }
                    sheets.add(ss);
                }
            }
        }

        if (quick) {
            oldQuick = ss;
        } else {
            oldCustom = ss;
        }
    }

    private void reset() {
        for (Window w: Window.getWindows()) {
            Scene scene = w.getScene();
            if (scene != null) {
                ObservableList<String> sheets = scene.getStylesheets();
                if (oldCustom != null) {
                    sheets.remove(oldCustom);
                }
                if (oldQuick != null) {
                    sheets.remove(oldQuick);
                }
            }
        }
        oldCustom = null;
        oldQuick = null;
    }
}
