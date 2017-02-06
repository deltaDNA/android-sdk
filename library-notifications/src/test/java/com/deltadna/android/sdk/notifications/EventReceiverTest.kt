package com.deltadna.android.sdk.notifications

import android.content.Intent
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class EventReceiverTest {
    
    private var uut = spy(EventReceiverImpl())
    private val context = RuntimeEnvironment.application
    
    @Before
    fun before() {
        uut = spy(EventReceiverImpl())
    }
    
    @Test
    fun onRegistered() {
        uut.onReceive(
                context,
                Intent(Actions.REGISTERED)
                        .putExtra(Actions.REGISTRATION_TOKEN, "token"))
        
        verify(uut).onRegistered(eq(context), eq("token"))
    }
    
    @Test
    fun onMessageReceived() {
        val message = mock<PushMessage>()
        uut.onReceive(
                context,
                Intent(Actions.MESSAGE_RECEIVED)
                        .putExtra(Actions.PUSH_MESSAGE, message))
        
        verify(uut).onMessageReceived(eq(context), eq(message))
    }
    
    @Test
    fun onNotificationPosted() {
        val info = mock<NotificationInfo>()
        uut.onReceive(
                context,
                Intent(Actions.NOTIFICATION_POSTED)
                        .putExtra(Actions.NOTIFICATION_INFO, info))
        
        verify(uut).onNotificationPosted(eq(context), eq(info))
    }
    
    @Test
    fun onNotificationOpened() {
        val info = mock<NotificationInfo>()
        uut.onReceive(
                context,
                Intent(Actions.NOTIFICATION_OPENED)
                        .putExtra(Actions.NOTIFICATION_INFO, info))
        
        verify(uut).onNotificationOpened(eq(context), eq(info))
    }
    
    @Test
    fun onNotificationDismissed() {
        val info = mock<NotificationInfo>()
        uut.onReceive(
                context,
                Intent(Actions.NOTIFICATION_DISMISSED)
                        .putExtra(Actions.NOTIFICATION_INFO, info))
        
        verify(uut).onNotificationDismissed(eq(context), eq(info))
    }
    
    private open class EventReceiverImpl : EventReceiver()
}