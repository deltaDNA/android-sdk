/*
 * Copyright (c) 2018 deltaDNA Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.deltadna.android.sdk.triggers


import com.deltadna.android.sdk.EventTriggeredCampaignMetricStore
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@RunWith(RobolectricTestRunner::class)
class ExecutionCountTriggerConditionTest {

    lateinit var uut: ExecutionCountTriggerCondition
    private lateinit var metricsMock: EventTriggeredCampaignMetricStore
    private var campaignId = 123L

    @Before
    fun setup() {
        metricsMock = mock()
    }

    @Test
    fun `canExecute when execution count is equal to desired amount`() {
        whenever(metricsMock.getETCExecutionCount(campaignId)).thenReturn(1)
        uut = ExecutionCountTriggerCondition(1, metricsMock, campaignId)
        assertTrue { uut.canExecute() }
    }

    @Test
    fun `canExecute is false when execution count is not equal to desired amount`() {
        whenever(metricsMock.getETCExecutionCount(campaignId)).thenReturn(2)
        uut = ExecutionCountTriggerCondition(1, metricsMock, campaignId)
        assertFalse { uut.canExecute() }
    }


}
