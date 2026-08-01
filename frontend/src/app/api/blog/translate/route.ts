import { NextRequest, NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const TRANSLATE_BACKEND =
  process.env.TRANSLATE_BACKEND_URL || "http://localhost:8090";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const upstream = await fetch(
      `${TRANSLATE_BACKEND}/api/v1/blog/translate`,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
          "X-Request-Source": "api-gateway",
        },
        body: JSON.stringify(body),
        cache: "no-store",
      },
    );

    const text = await upstream.text();
    const contentType =
      upstream.headers.get("content-type") || "application/json";
    return new NextResponse(text, {
      status: upstream.status,
      headers: { "Content-Type": contentType },
    });
  } catch (err: any) {
    return NextResponse.json(
      {
        success: false,
        message: `Translate proxy failed: ${err?.message || String(err)}`,
        errors: [],
        data: null,
      },
      { status: 502 },
    );
  }
}
