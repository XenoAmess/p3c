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
package com.alibaba.p3c.idea.lifecycle

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.alibaba.p3c.idea.component.AliProjectComponent
import com.alibaba.p3c.idea.pmd.SourceCodeProcessor
import com.alibaba.p3c.idea.inspection.AliPmdInspectionInvoker

/**
 * Plugin lifecycle manager for dynamic plugin support.
 * Handles cleanup when plugin is unloaded without IDE restart.
 *
 * @author XenoAmess
 */
class P3cPluginLifecycle : DynamicPluginListener {

    companion object {
        const val PLUGIN_ID = "com.alibaba.p3c.xenoamess"
    }

    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        if (pluginDescriptor.pluginId.idString != PLUGIN_ID) {
            return
        }

        cleanupResources()
    }

    override fun pluginUnloaded(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        // Additional cleanup if needed after plugin is unloaded
    }

    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        // Initialization when plugin is loaded dynamically
    }

    /**
     * Cleanup all resources to prevent memory leaks and allow proper plugin unloading.
     */
    private fun cleanupResources() {
        // Clear all PMD caches
        SourceCodeProcessor.clearAllCaches()

        // Clear file violation caches
        AliPmdInspectionInvoker.clearAllFileViolationsCache()

        // Cleanup for each open project
        ProjectManager.getInstance().openProjects.forEach { project ->
            cleanupProject(project)
        }
    }

    /**
     * Cleanup resources for a specific project.
     */
    private fun cleanupProject(project: Project) {
        try {
            // Remove VirtualFileListener for this project
            val projectComponent = project.getComponent(AliProjectComponent::class.java)
            projectComponent?.let {
                VirtualFileManager.getInstance().removeVirtualFileListener(it.getVirtualFileListener())
            }
        } catch (ignored: Exception) {
            // Component might not be initialized or already disposed
        }
    }
}
