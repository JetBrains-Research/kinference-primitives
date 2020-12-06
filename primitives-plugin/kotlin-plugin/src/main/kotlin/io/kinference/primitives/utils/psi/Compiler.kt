package io.kinference.primitives.utils.psi

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil

internal val CompilerConfiguration.collector: MessageCollector
    get() = this.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

internal fun <T: Any> T.forced(): T = ForceResolveUtil.forceResolveAllContents(this)
