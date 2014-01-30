/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.glass.ui.monocle.input;

import com.sun.glass.ui.monocle.input.devices.TestTouchDevice;
import com.sun.glass.ui.monocle.input.devices.TestTouchDevices;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.util.Collection;

public class TouchEventLookaheadTest extends ParameterizedTestBase {

    public TouchEventLookaheadTest(TestTouchDevice device) {
        super(device);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return TestTouchDevices.getTouchDeviceParameters(1);
    }

    /** Merge together similar moves */
    @Test
    public void mergeMoves() throws Exception {
        Assume.assumeTrue(TestApplication.isMonocle());
        TestApplication.showFullScreenScene();
        TestApplication.addMouseListeners();
        TestApplication.addTouchListeners();
        TestLog.reset();
        Rectangle2D r = Screen.getPrimary().getBounds();
        final int width = (int) r.getWidth();
        final int height = (int) r.getHeight();
        final int x1 = (int) Math.round(width * 0.1);
        final int y1 = (int) Math.round(height * 0.1);
        final int x2 = (int) Math.round(width * 0.9);
        final int y2 = (int) Math.round(height * 0.9);
        // Push events while on the event thread, making sure that events
        // will be buffered up and enabling filtering to take place
        TestRunnable.invokeAndWait(() -> {
            int p = device.addPoint(x1, y1);
            device.sync();
            for (int x = x1; x <= x2; x += (x2 - x1) / 11) {
                device.setPoint(p, x, y1);
                device.sync();
            }
            for (int y = y1; y <= y2; y += (y2 - y1) / 11) {
                device.setPoint(p, x2, y);
                device.sync();
            }
            device.setPoint(p, x2, y2);
            device.sync();
            device.removePoint(p);
            device.sync();
        });
        // Check that the initial point reported is correct
        TestLog.waitForLog("Mouse pressed: " + x1 + ", " + y1, 3000);
        TestLog.waitForLog("Touch pressed: " + x1 + ", " + y1, 3000);
        // Check that the final point reported is correct
        TestLog.waitForLog("Mouse released: " + x2 + ", " + y2, 3000);
        TestLog.waitForLog("Touch released: " + x2 + ", " + y2, 3000);
        // Check that there was only one move in between
        TestLog.waitForLog("Mouse dragged: " + x2 + ", " + y2, 3000);
        TestLog.waitForLog("Touch moved: " + x2 + ", " + y2, 3000);
        Assert.assertEquals(1, TestLog.countLogContaining("Mouse dragged"));
        Assert.assertEquals(1, TestLog.countLogContaining("Touch moved"));
    }
}