from django.shortcuts import render
from django.http import JsonResponse
from .models import ObjectSighting

# Create your views here.

def index(request):
    """Renders the map page."""
    return render(request, 'index.html')

def get_item_location(request, item_name):
    """API Endpoint to find the last seen location."""
    # Use Django ORM to find the most recent sighting
    sightings = ObjectSighting.objects.filter(object_name=item_name).order_by('-timestamp')

    if sightings.exists():
        data = [
            {"image_url": s.image_url, "timestamp": s.timestamp.isoformat()}
            for s in sightings
        ]
        return JsonResponse({
            "status": "found",
            "data": data
        })
    else:
        return JsonResponse({"status": "not_found", "data": []})
