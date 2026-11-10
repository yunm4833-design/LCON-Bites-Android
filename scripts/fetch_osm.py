#!/usr/bin/env python3
"""Build a *required* offline road/food POI snapshot from OSM Overpass.
No public tile scraping. Fail the build rather than shipping an empty map.
OSM contributors, ODbL 1.0: https://www.openstreetmap.org/copyright
"""
import json, os, time, urllib.request, urllib.parse, math
BBOX=(53.784,-1.559,53.813,-1.512)
CENTER=(53.7986,-1.5354)
OUT="app/src/main/assets"
os.makedirs(OUT,exist_ok=True)
query='''[out:json][timeout:100];(way["highway"](53.784,-1.559,53.813,-1.512);nwr["amenity"~"^(restaurant|cafe|fast_food|food_court|ice_cream)$"](53.784,-1.559,53.813,-1.512);nwr["shop"~"^(bakery|deli|pastry|confectionery)$"](53.784,-1.559,53.813,-1.512););out center geom;'''
hosts=[
"https://overpass.kumi.systems/api/interpreter",
"https://overpass.nchc.org.tw/api/interpreter",
"https://overpass.private.coffee/api/interpreter",
"https://overpass-api.de/api/interpreter",
"https://overpass.osm.ch/api/interpreter",
]
raw=None
for host in hosts:
    for attempt in range(2):
        try:
            req=urllib.request.Request(host,data=urllib.parse.urlencode({"data":query}).encode(),headers={"User-Agent":"LCON-Bites-Android/0.3 (noncommercial educational offline data build)","Content-Type":"application/x-www-form-urlencoded"},method="POST")
            with urllib.request.urlopen(req,timeout=125) as response: raw=json.load(response)
            if len(raw.get("elements",[]))<300: raise RuntimeError("Incomplete OSM extract")
            print("OSM source:",host,"elements:",len(raw["elements"]),flush=True)
            break
        except Exception as e:
            print("OSM endpoint failed:",host,"attempt",attempt+1,str(e),flush=True)
            time.sleep(3)
    if raw:break
if raw is None:raise SystemExit("FAILED: no reliable OSM source. Refusing to publish an empty offline map.")
roads=[]; places=[];seen=set()
def distance(lat,lon):
    a=math.radians(lat-CENTER[0]); b=math.radians(lon-CENTER[1])
    return 6371000*math.hypot(a,b*math.cos(math.radians(CENTER[0])))
for e in raw["elements"]:
    t=e.get("tags",{})
    geom=e.get("geometry",[])
    if t.get("highway") and len(geom)>1:
        roads.append({"kind":t["highway"],"points":[[round(p["lon"],6),round(p["lat"],6)] for p in geom]})
    food=t.get("amenity") in {"restaurant","cafe","fast_food","food_court","ice_cream"} or t.get("shop") in {"bakery","deli","pastry","confectionery"}
    if not food or not t.get("name"):continue
    p=e if "lat" in e else e.get("center",{})
    lat,lon=p.get("lat"),p.get("lon")
    if lat is None or lon is None:continue
    d=distance(lat,lon)
    # 1500 m straight-line candidate area. Actual walking network
    # may be longer; do NOT describe these as verified 15-minute walking routes.
    if d>1500:continue
    key=(t["name"].casefold().strip(),round(lat,4),round(lon,4))
    if key in seen:continue
    seen.add(key)
    address=" ".join(x for x in [t.get("addr:housenumber",""),t.get("addr:street",""),t.get("addr:postcode","")] if x).strip()
    places.append({"id":str(e["type"])+str(e["id"]),"name":t["name"],"category":t.get("cuisine") or t.get("amenity") or t.get("shop") or "Food","address":address or "Leeds city centre","lat":round(lat,6),"lon":round(lon,6),"distance_m":round(d),"osm_id":str(e["type"])+"/"+str(e["id"])})
if len(roads)<100 or len(places)<35:
    raise SystemExit(f"FAILED: incomplete data: {len(roads)} roads, {len(places)} food POIs. No APK will be published.")
places.sort(key=lambda x:(x["distance_m"],x["name"]))
with open(OUT+"/leeds_roads.json","w") as f:json.dump({"roads":roads,"source":"© OpenStreetMap contributors; ODbL 1.0"},f,separators=(",",":"))
with open(OUT+"/leeds_food.json","w") as f:json.dump({"places":places,"source":"© OpenStreetMap contributors; ODbL 1.0","scope":"1500m straight-line candidate radius, NOT verified 15-minute walking routes"},f,separators=(",",":"))
print("VALIDATED OFFLINE SNAPSHOT:",len(roads),"roads,",len(places),"food places",flush=True)
