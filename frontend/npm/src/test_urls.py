import urllib.request
urls = [
    "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f",
    "https://images.unsplash.com/photo-1434389670869-c6e4fe0f5125",
    "https://images.unsplash.com/photo-1496747611176-843222e1e57c",
    "https://images.unsplash.com/photo-1509319117193-57bab727e09d",
    "https://images.unsplash.com/photo-1529139574466-a303027c1d8b",
    "https://images.unsplash.com/photo-1550614000-4b95d4662d55",
    "https://images.unsplash.com/photo-1485230895905-eb40f6b60db4",
]
for url in urls:
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        res = urllib.request.urlopen(req)
        print(url, res.getcode())
    except Exception as e:
        print(url, e)
