package com.example.onboarder.tools

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Description
import java.util.function.Function

data class PtoRequest(val dummy: String = "")
data class PtoBalance(
    val vacationDays: Double,
    val sickDays: Double,
    val personalDays: Double,
    val totalAvailable: Double
)

/**
 * PTO balance tool for AI function calling.
 * Simulates interaction with company HR API to retrieve employee absence balances.
 */
@Configuration
class PtoBalanceTool {

    @Bean
    @Description("Get current user's PTO (Paid Time Off) balance including vacation, sick, and personal days. Use this whenever asked about PTO, vacation, sick leave, or absence balances.")
    fun getPtoBalance(): Function<PtoRequest, PtoBalance> {
        return Function {
            // Stub implementation - simulates company API call
            // Employee identity would be fetched from security context/session
            fetchPtoBalance()
        }
    }

    private fun fetchPtoBalance(): PtoBalance {
        // Simulating API response with realistic data
        return PtoBalance(
            vacationDays = 15.5,
            sickDays = 8.0,
            personalDays = 3.0,
            totalAvailable = 26.5
        )
    }
}
