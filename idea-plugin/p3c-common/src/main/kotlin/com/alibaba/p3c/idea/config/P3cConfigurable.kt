/*
 * Copyright 1999-2017 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.p3c.idea.config

import com.alibaba.p3c.idea.compatible.inspection.InspectionProfileService
import com.alibaba.p3c.idea.compatible.inspection.Inspections
import com.alibaba.p3c.idea.inspection.AliBaseInspection
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiManager
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Settings page for P3C plugin configuration.
 * Provides GUI for enabling/disabling plugin without IDE restart.
 *
 * @author XenoAmess
 */
class P3cConfigurable(private val project: Project) : Configurable {

    private var mainPanel: JPanel? = null
    private var enablePluginCheckBox: JCheckBox? = null
    private var enableRealtimeCheckBox: JCheckBox? = null

    override fun getDisplayName(): String = "P3C Code Guidelines"

    override fun createComponent(): JComponent? {
        mainPanel = JPanel(BorderLayout(10, 10))
        mainPanel?.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val settingsPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(5, 5, 5, 5)
            weightx = 1.0
            gridx = 0
        }

        // Plugin enable/disable checkbox
        enablePluginCheckBox = JCheckBox("Enable P3C Code Guidelines plugin")
        enablePluginCheckBox?.toolTipText = "Enable or disable all P3C inspections without IDE restart"

        gbc.gridy = 0
        gbc.gridwidth = 2
        settingsPanel.add(enablePluginCheckBox, gbc)

        // Description label
        val descLabel = JLabel("<html><body style='width: 400px;'>" +
            "When disabled, all P3C inspections will be turned off immediately. " +
            "No IDE restart is required for this change to take effect." +
            "</body></html>")
        descLabel.font = descLabel.font.deriveFont(descLabel.font.size2D - 1)

        gbc.gridy = 1
        gbc.insets = Insets(0, 25, 15, 5)
        settingsPanel.add(descLabel, gbc)

        // Real-time inspection checkbox
        enableRealtimeCheckBox = JCheckBox("Enable real-time inspection")
        enableRealtimeCheckBox?.toolTipText = "Enable or disable real-time code inspection as you type"

        gbc.gridy = 2
        gbc.insets = Insets(5, 5, 5, 5)
        settingsPanel.add(enableRealtimeCheckBox, gbc)

        // Real-time description label
        val realtimeDescLabel = JLabel("<html><body style='width: 400px;'>" +
            "When enabled, P3C inspections run automatically while editing. " +
            "Disabling this can improve editor performance on large files." +
            "</body></html>")
        realtimeDescLabel.font = realtimeDescLabel.font.deriveFont(realtimeDescLabel.font.size2D - 1)

        gbc.gridy = 3
        gbc.insets = Insets(0, 25, 5, 5)
        settingsPanel.add(realtimeDescLabel, gbc)

        // Fill remaining space
        gbc.gridy = 4
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        settingsPanel.add(JPanel(), gbc)

        mainPanel?.add(settingsPanel, BorderLayout.NORTH)

        reset()

        return mainPanel
    }

    override fun isModified(): Boolean {
        val config = project.getService(SmartFoxProjectConfig::class.java)
        val pluginEnabled = !config.pluginGloballyDisabled
        val realtimeEnabled = config.realtimeInspectionEnabled

        return enablePluginCheckBox?.isSelected != pluginEnabled ||
                enableRealtimeCheckBox?.isSelected != realtimeEnabled
    }

    override fun apply() {
        val config = project.getService(SmartFoxProjectConfig::class.java)

        val wasGloballyDisabled = config.pluginGloballyDisabled
        val pluginEnabled = enablePluginCheckBox?.isSelected ?: true
        config.pluginGloballyDisabled = !pluginEnabled

        val wasRealtimeEnabled = config.realtimeInspectionEnabled
        config.realtimeInspectionEnabled = enableRealtimeCheckBox?.isSelected ?: true

        // Apply changes immediately if state changed
        if (wasGloballyDisabled != config.pluginGloballyDisabled) {
            applyPluginState(project, !config.pluginGloballyDisabled)
        }

        // Refresh all open files to apply changes
        if (wasGloballyDisabled != config.pluginGloballyDisabled ||
            wasRealtimeEnabled != config.realtimeInspectionEnabled) {
            FileEditorManager.getInstance(project).openFiles.forEach { file ->
                val psiFile = PsiManager.getInstance(project).findFile(file)
                if (psiFile != null) {
                    DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
                }
            }
        }
    }

    override fun reset() {
        val config = project.getService(SmartFoxProjectConfig::class.java)
        enablePluginCheckBox?.isSelected = !config.pluginGloballyDisabled
        enableRealtimeCheckBox?.isSelected = config.realtimeInspectionEnabled
    }

    /**
     * Apply plugin enabled/disabled state
     */
    private fun applyPluginState(project: Project, enable: Boolean) {
        val tools = Inspections.aliInspections(project) {
            it.tool is AliBaseInspection
        }
        InspectionProfileService.toggleInspection(project, tools, !enable)
    }
}
