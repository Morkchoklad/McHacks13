from django.db import models

# Create your models here.
class ObjectSighting(models.Model):
    object_name = models.CharField(max_length=100)
    image_url = models.CharField(max_length=255)
    timestamp = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"{self.object_name} at {self.timestamp}"
