"""Rebuild the bundled survey-food index from the pinned USDA CSV archive."""
import csv
import hashlib
import io
import sys
import zipfile
from collections import defaultdict
from pathlib import Path

archive = Path(sys.argv[1])
expected = "5ccc25ec2777a8982fbb61378a42f415316173eb11e48c9a8ba4cb19f5a4f29c"
if hashlib.sha256(archive.read_bytes()).hexdigest() != expected:
    raise ValueError("Archive differs from the documented USDA snapshot")
assets = Path(__file__).resolve().parents[1] / "app/src/main/assets"
with (assets / "nutrients.tsv").open() as source:
    numbers = [r["number"] for r in csv.DictReader(source, delimiter="\t")]
with zipfile.ZipFile(archive) as z:
    def rows(table):
        path = next(p for p in z.namelist() if p.endswith("/" + table + ".csv"))
        with z.open(path) as binary:
            yield from csv.DictReader(io.TextIOWrapper(binary, encoding="utf-8-sig"))

    nutrients = defaultdict(dict)
    for r in rows("food_nutrient"):
        if r["nutrient_id"] in numbers:
            nutrients[r["fdc_id"]][r["nutrient_id"]] = r["amount"]
    with (assets / "survey_foods.tsv").open("w", newline="") as destination:
        writer = csv.writer(destination, delimiter="\t", lineterminator="\n", quoting=csv.QUOTE_NONE, quotechar=None)
        writer.writerow(["fdc_id", "name", *numbers])
        for r in rows("food"):
            data = nutrients[r["fdc_id"]]
            if all(n in data for n in numbers[:4]):
                writer.writerow([r["fdc_id"], " ".join(r["description"].split()), *(data.get(n, "") for n in numbers)])
