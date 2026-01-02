#!/usr/bin/env node

const axios = require('axios');

const BASE_URL = 'http://localhost:8080';

async function getMetrics() {
    try {
        const response = await axios.get(`${BASE_URL}/api/metrics`);
        return response.data;
    } catch (error) {
        console.error('❌ Failed to get metrics:', error.message);
        return null;
    }
}

async function getActiveWebSocketSessions() {
    try {
        const response = await axios.get(`${BASE_URL}/api/metrics/sessions`);
        return response.data;
    } catch (error) {
        console.error('❌ Failed to get sessions:', error.message);
        return [];
    }
}

function formatDuration(seconds) {
    if (seconds < 60) return `${seconds}s`;
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}m ${remainingSeconds}s`;
}

async function displayActiveSessionsDetailed() {
    const sessions = await getActiveWebSocketSessions();
    const activeSessions = sessions.filter(session => !session.disconnectedAt);
    
    console.log(`\n📊 Active WebSocket Sessions: ${activeSessions.length}`);
    
    if (activeSessions.length > 0) {
        console.log('┌─────────────────────────────────────────────────────────────────────────────┐');
        console.log('│                           Active Session Details                           │');
        console.log('├─────────────────────────────────────────────────────────────────────────────┤');
        
        activeSessions.forEach((session, index) => {
            const sessionIdShort = session.sessionId.substring(0, 8) + '...';
            const connectedAt = new Date(session.connectedAt);
            const now = new Date();
            const durationSeconds = Math.floor((now - connectedAt) / 1000);
            
            console.log(`│ ${index + 1}. Session: ${sessionIdShort.padEnd(12)} │`);
            console.log(`│    Bus: ${session.busNumber} ${session.direction.padEnd(20)} │`);
            console.log(`│    Connected: ${connectedAt.toLocaleTimeString().padEnd(15)} │`);
            console.log(`│    Duration: ${formatDuration(durationSeconds).padEnd(16)} │`);
            
            if (index < activeSessions.length - 1) {
                console.log('├─────────────────────────────────────────────────────────────────────────────┤');
            }
        });
        
        console.log('└─────────────────────────────────────────────────────────────────────────────┘');
    } else {
        console.log('✅ No active WebSocket sessions');
    }
}

async function monitorWebSocketCleanup() {
    console.log('🧪 WebSocket Cleanup Monitoring Tool');
    console.log('=====================================\n');
    
    console.log('📊 Step 1: Checking initial state...');
    await displayActiveSessionsDetailed();
    
    console.log('\n🔄 Step 2: Connect with Flutter app and start tracking a bus...');
    console.log('   Instructions:');
    console.log('   1. Open Flutter app');
    console.log('   2. Select "Track Bus"');
    console.log('   3. Choose C5 bus');
    console.log('   4. Select direction (Northbound/Southbound)');
    console.log('   5. Select a bus stop');
    console.log('   6. Wait for tracking to start');
    console.log('\nPress Enter when tracking has started...');
    
    // Wait for user input
    await new Promise(resolve => {
        process.stdin.once('data', () => resolve());
    });
    
    console.log('\n📊 Checking sessions after connection...');
    await displayActiveSessionsDetailed();
    
    console.log('\n🚌 Step 3: Wait for bus to arrive or manually trigger cleanup...');
    console.log('   Instructions:');
    console.log('   1. Wait for the bus to reach destination (distance < 100m)');
    console.log('   2. The "Bus Has Arrived!" modal should appear');
    console.log('   3. Click either "Go Home" or "Track Another Bus"');
    console.log('\nPress Enter after the arrival modal has been dismissed...');
    
    // Wait for user input
    await new Promise(resolve => {
        process.stdin.once('data', () => resolve());
    });
    
    console.log('\n📊 Checking sessions after cleanup should have occurred...');
    await displayActiveSessionsDetailed();
    
    // Check multiple times to see if cleanup happens with delay
    for (let i = 1; i <= 5; i++) {
        console.log(`\n⏰ Waiting ${i * 2} seconds and checking again...`);
        await new Promise(resolve => setTimeout(resolve, 2000));
        await displayActiveSessionsDetailed();
    }
    
    console.log('\n🎯 Test completed!');
    console.log('\n📋 Summary:');
    console.log('   - If sessions are still active after bus arrival, there\'s a cleanup issue');
    console.log('   - If sessions are properly closed, the cleanup is working correctly');
    console.log('   - Check the Flutter console logs for debug messages about cleanup process');
}

// Handle Ctrl+C gracefully
process.on('SIGINT', () => {
    console.log('\n\n👋 Monitoring stopped by user');
    process.exit(0);
});

// Start monitoring
monitorWebSocketCleanup().catch(error => {
    console.error('❌ Monitoring failed:', error);
    process.exit(1);
});