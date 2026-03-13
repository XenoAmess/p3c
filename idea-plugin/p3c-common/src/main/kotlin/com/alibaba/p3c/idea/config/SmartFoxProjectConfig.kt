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

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 *
 *
 * @author caikang
 * @date 2017/03/01
 */
@State(name = "SmartFoxProjectConfig", storages = [com.intellij.openapi.components.Storage(file = "smartfox_info.xml")])
class SmartFoxProjectConfig : PersistentStateComponent<SmartFoxProjectConfig> {
    var projectInspectionClosed = false

    /**
     * Global plugin disable state for dynamic plugin support.
     * When true, all P3C inspections are completely disabled without IDE restart.
     */
    var pluginGloballyDisabled = false

    /**
     * Individual rule disable list.
     * Contains short names of disabled rules.
     */
    var disabledRules: MutableSet<String> = mutableSetOf()

    /**
     * Enable/disable real-time inspection.
     */
    var realtimeInspectionEnabled = true

    override fun getState(): SmartFoxProjectConfig? {
        return this
    }

    override fun loadState(state: SmartFoxProjectConfig) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
