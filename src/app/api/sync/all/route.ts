import { NextResponse } from 'next/server';

// Enable dynamic API routes
export const dynamic = 'force-dynamic';

export async function POST() {
  try {
    // Proxy to backend API
    const response = await fetch('http://localhost:8080/api/sync/all', {
      method: 'POST',
    });
    const data = await response.json();
    
    return NextResponse.json(data, { status: response.status });
  } catch (error) {
    console.error('Error syncing all weather:', error);
    return NextResponse.json(
      { error: 'Failed to sync all weather data' },
      { status: 500 }
    );
  }
}