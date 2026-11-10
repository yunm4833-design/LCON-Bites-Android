#!/usr/bin/env python3
"""Fetch a small, geographically bounded OSM roads extract for bundling offline.

OpenStreetMap data © OpenStreetMap contributors, ODbL 1.0.
Overpass is queried once per build; no public raster tile server is scraped.
"""
import json
import os
import urllib.parse
import urllib.request

DEST = "app/src/main/assets/leeds_roads.json"
os.makedirs(os.path.dirname(DEST), exist_ok=True)
query = """[out:json][timeout:90];way["highway"](53.7876,-1.5544,53.8096,-1.5164);out geom;"""
url = "https://overpass.kumi.systems/api/interpreter?data=" + urllib.parse.quote(query)
fallback = {"roads": [], "source": "Road download unavailable"}
try:
    request = urllib.request.Request(url, headers={"User-Agent": "LCON-Bites-offline-build/0.2 (educational project)"})
    with urllib.request.urlopen(request, timeout=110) as response:
        raw = json.load(response)
    roads = []
    for element in raw.get("elements", []):
        geometry = element.get("geometry", [])
        if len(geometry) < 2:
            continue
        tags = element.get("tags", {})
        kind = tags.get("highway", "")
        if kind in {"footway", "path", "steps", "cycleway", "service", "residential", "tertiary", "secondary", "primary", "pedestrian", "living_street", "unclassified", "trunk"}:
            roads.append({
                "kind": kind,
                "points": [[round(p["lon"], 6), round(p["lat"], 6)] for p in geometry]
            })
    if not roads:
        raise RuntimeError("No OSM roads returned")
    data = {"roads": roads, "source": "OpenStreetMap contributors (ODbL)"}
    print(f"Bundling {len(roads)} OSM roads")
except Exception as error:
    print(f"Warning: OSM download failed: {error}")
    data = fallback
with open(DEST, "w", encoding="utf-8") as file:
    json.dump(data, file, separators=(",", ":"))
