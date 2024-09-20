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
package com.alibaba.p3c.idea.i18n

import com.alibaba.p3c.idea.config.P3cConfig
import com.xenoamess.p3c.pmd.I18nResources
import com.alibaba.smartfox.idea.common.util.getService
import com.intellij.AbstractBundle
import com.intellij.openapi.application.CachedSingletonsRegistry
import java.util.Locale
import java.util.ResourceBundle
import java.util.function.Supplier

/**
 *
 *
 * @author caikang
 * @date 2017/06/20
 */
object P3cBundle {
    private val p3cConfigSupplier: Supplier<P3cConfig> =
        CachedSingletonsRegistry.lazy {
            P3cConfig::class.java.getService()
        }
    private val resourceBundleSupplier =
        CachedSingletonsRegistry.lazy {
            ResourceBundle.getBundle(
                "messages.P3cBundle",
                Locale(p3cConfigSupplier.get().locale), I18nResources.XmlControl()
            )
        }

    fun getMessage(key: String): String {
        return resourceBundleSupplier.get().getString(key).trim()
    }

    fun message(key: String, vararg params: Any): String {
        return AbstractBundle.message(resourceBundleSupplier.get(), key, *params).trim()
    }
}
