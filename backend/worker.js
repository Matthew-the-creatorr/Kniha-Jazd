/**
 * Cloudflare Worker template for server-side Gemini image generation.
 * Deploy from an authorized developer account with TWO secret variables:
 * GEMINI_API_KEY and APP_ACCESS_TOKEN. Never place secrets in Android source.
 * No public unrestricted image generation.
 */
export default {
  async fetch(request, env) {
    const path = new URL(request.url).pathname;
    if (path === "/health" && request.method === "GET") {
      return Response.json({ status: "ok", configured: !!(env.GEMINI_API_KEY && env.APP_ACCESS_TOKEN) });
    }
    if (path !== "/generate" || request.method !== "POST") {
      return new Response("Not found", { status: 404 });
    }
    if (!env.APP_ACCESS_TOKEN || !env.GEMINI_API_KEY) {
      return new Response("Backend not configured", { status: 503 });
    }
    if (request.headers.get("Authorization") !== "Bearer " + env.APP_ACCESS_TOKEN) {
      return new Response("Unauthorized", { status: 401 });
    }
    try {
      const data = await request.json();
      const names = ["brand", "model", "year", "color", "finish"];
      const v = {};
      for (const name of names) {
        if (typeof data[name] !== "string" || data[name].length > 90 || data[name].length < 1)
          return new Response("Invalid vehicle input", { status: 400 });
        v[name] = data[name].replace(/[\\r\\n<>]/g, " ").trim();
      }
      const prompt = [
        "Create ONE ultra photorealistic VERTICAL 9:16 premium automotive mobile wallpaper.",
        "The CAR MUST BE", v.brand, v.model, "model year", v.year + ",",
        "factory accurate shape and generation,",
        v.color, "body color with", v.finish, "paint surface.",
        "Beautiful luxurious BLACK automotive showroom with elegant white LED ribbons on ceiling",
        "and premium dark reflective floor. Three-quarter FRONT view slightly from ABOVE looking downward",
        "so the entire car is prominently visible at the TOP to CENTER of the image.",
        "Leave the bottom quarter relatively dark for semi-transparent app cards.",
        "Physically accurate proportions and lights, highly realistic commercial photography.",
        "One car only, no text, no UI, no license plates, no people, no watermark."
      ].join(" ");
      const upstream = await fetch(
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image:generateContent",
        {
          method: "POST",
          headers: { "Content-Type": "application/json", "x-goog-api-key": env.GEMINI_API_KEY },
          body: JSON.stringify({
            contents: [{ parts: [{ text: prompt }] }],
            generationConfig: { responseModalities: ["IMAGE"], imageConfig: { aspectRatio: "9:16" } }
          })
        }
      );
      if (!upstream.ok) {
        console.error("Gemini image request failed", upstream.status);
        return new Response("Image provider unavailable (HTTP " + upstream.status + ")", { status: 502 });
      }
      const json = await upstream.json();
      const parts = json.candidates?.[0]?.content?.parts || [];
      const item = parts.find(x => x.inlineData?.data);
      if (!item) return new Response("No image returned", { status: 502 });
      const binary = Uint8Array.from(atob(item.inlineData.data), c => c.charCodeAt(0));
      return new Response(binary, {
        status: 200,
        headers: {
          "Content-Type": item.inlineData.mimeType || "image/png",
          "Cache-Control": "no-store"
        }
      });
    } catch (e) {
      console.error("Image worker error", String(e).slice(0,160));
      return new Response("Background generation failed", { status: 500 });
    }
  }
};
