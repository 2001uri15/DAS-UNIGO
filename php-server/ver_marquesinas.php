<!DOCTYPE html>
<html>
<head>
    <title>Mapa de Paradas de Bus</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
    <style>
        #map { height: 100vh; width: 100%; }
        .stop-info {
            padding: 10px;
            font-family: Arial, sans-serif;
        }
        .stop-info h3 {
            margin-top: 0;
            color: #2c3e50;
        }
    </style>
</head>
<body>
    <div id="map"></div>

    <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
    <script>
        // Crear el mapa
        const map = L.map('map').setView([42.85, -2.67], 13); // Coordenadas iniciales (ajustar según necesidad)
        
        // Añadir capa de OpenStreetMap
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);
        
        // Icono personalizado para las paradas
        const busStopIcon = L.icon({
            iconUrl: 'https://cdn-icons-png.flaticon.com/512/3473/3473477.png', // URL de tu icono personalizado
            iconSize: [32, 32], // tamaño del icono
            iconAnchor: [16, 32], // punto del icono que corresponde a la ubicación
            popupAnchor: [0, -32] // punto desde el que se abre el popup
        });
        
        // Obtener datos de las paradas desde el PHP
        fetch('marquesinas.php') // Reemplaza con la ruta correcta a tu PHP
            .then(response => response.json())
            .then(stops => {
                stops.forEach(stop => {
                    // Añadir marcador para cada parada
                    const marker = L.marker([stop.stop_lat, stop.stop_lon], {
                        icon: busStopIcon
                    }).addTo(map);
                    
                    // Crear contenido para el popup
                    const popupContent = `
                        <div class="stop-info">
                            <h3>${stop.stop_name}</h3>
                            <p><strong>ID:</strong> ${stop.stop_id}</p>
                            <p><strong>Nombre:</strong> ${stop.stop_name}</p>
                            <p><strong>Latitud:</strong> ${stop.stop_lat}</p>
                            <p><strong>Longitud:</strong> ${stop.stop_lon}</p>
                        </div>
                    `;
                    
                    // Asignar popup al marcador
                    marker.bindPopup(popupContent);
                });
            })
            .catch(error => {
                console.error('Error al cargar las paradas:', error);
            });
    </script>
</body>
</html>
