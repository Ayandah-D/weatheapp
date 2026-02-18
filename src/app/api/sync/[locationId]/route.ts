import { NextResponse } from "next/server";

// Enable dynamic API routes
export const dynamic = 'force-dynamic';

export async function POST(
  request: Request,
  { params }: { params: Promise<{ locationId: string }> }
) {
  try {
    // unwrap params first
    const { locationId } = await params;

    // Proxy to backend API
    const response = await fetch(`http://localhost:8080/api/sync/${locationId}`, {
      method: 'POST',
    });
    const data = await response.json();
    
    return NextResponse.json(data, { status: response.status });
  } catch (error) {
    console.error('Error syncing weather:', error);
    return NextResponse.json(
      { error: 'Failed to sync weather data' },
      { status: 500 }
    );
  }
}
