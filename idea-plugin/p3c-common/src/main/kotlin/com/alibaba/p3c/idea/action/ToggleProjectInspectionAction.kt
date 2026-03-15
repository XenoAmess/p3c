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
package com.alibaba.p3c.idea.action

import com.alibaba.p3c.idea.compatible.inspection.InspectionProfileService
import com.alibaba.p3c.idea.compatible.inspection.Inspections
import com.alibaba.p3c.idea.config.SmartFoxProjectConfig
import com.alibaba.p3c.idea.i18n.P3cBundle
import com.alibaba.p3c.idea.inspection.AliBaseInspection
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiManager
import icons.P3cIcons

/**
 *
 * Open or close inspections
 * Enhanced for dynamic plugin support - allows toggling plugin without IDE restart
 *
 * @author caikang
 * @date 2017/03/14
 */
class ToggleProjectInspectionAction : AnAction() {
    val textKey = "com.alibaba.p3c.idea.action.ToggleProjectInspectionAction.text"

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val smartFoxConfig = project.getService(SmartFoxProjectConfig::class.java)

        // Toggle global plugin state
        smartFoxConfig.pluginGloballyDisabled = !smartFoxConfig.pluginGloballyDisabled

        // Apply the state change
        applyPluginState(project, !smartFoxConfig.pluginGloballyDisabled)

        // Show notification to user
        val message = if (smartFoxConfig.pluginGloballyDisabled) {
            P3cBundle.getMessage("com.alibaba.p3c.idea.action.ToggleProjectInspectionAction.disabled.notification")
                ?: "P3C plugin has been disabled"
        } else {
            P3cBundle.getMessage("com.alibaba.p3c.idea.action.ToggleProjectInspectionAction.enabled.notification")
                ?: "P3C plugin has been enabled"
        }

        NotificationGroupManager.getInstance()
            .getNotificationGroup("P3C Notifications")
            ?.createNotification(message, NotificationType.INFORMATION)
            ?.notify(project)
    }

    /**
     * Apply plugin enabled/disabled state
     */
    private fun applyPluginState(project: com.intellij.openapi.project.Project, enable: Boolean) {
        val tools = Inspections.aliInspections(project) {
            it.tool is AliBaseInspection
        }

        // Use the existing toggle mechanism
        InspectionProfileService.toggleInspection(project, tools, !enable)

        // Refresh all open files to apply changes immediately
        FileEditorManager.getInstance(project).openFiles.forEach { file ->
            val psiFile = PsiManager.getInstance(project).findFile(file)
            if (psiFile != null) {
                DaemonCodeAnalyzer.getInstance(project).restart(psiFile)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val smartFoxConfig = project.getService(SmartFoxProjectConfig::class.java)

        // Check global disabled state first, fall back to project level
        val isDisabled = smartFoxConfig.pluginGloballyDisabled || smartFoxConfig.projectInspectionClosed

        e.presentation.text = if (isDisabled) {
            e.presentation.icon = P3cIcons.PROJECT_INSPECTION_ON
            P3cBundle.getMessage("$textKey.open") ?: "Enable P3C Inspections"
        } else {
            e.presentation.icon = P3cIcons.PROJECT_INSPECTION_OFF
            P3cBundle.getMessage("$textKey.close") ?: "Disable P3C Inspections"
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

}
