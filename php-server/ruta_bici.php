<?php
// Conexión a la base de datos
$host = 'localhost';
$dbname = 'Xalarrazabal025_unigo';
$user = 'Xalarrazabal025';
$password = 'vRN7UMCCFV';

try {
    $conn = new PDO("mysql:host=$host;dbname=$dbname", $user, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Consulta para obtener todas las rutas con geometría válida
    $stmt = $conn->query("SELECT 
                            id, 
                            direccion, 
                            tipologia, 
                            IFNULL(calle, 'Sin nombre') as calle, 
                            longitud_metros, 
                            ST_AsGeoJSON(geometria) as geojson 
                          FROM bici_camino 
                          WHERE ST_IsValid(geometria) = 1");
    $rutas = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
} catch(PDOException $e) {
    die("Error de conexión: " . $e->getMessage());
}
?>

<!DOCTYPE html>
<html>
<head>
    <title>Visualización de Rutas Ciclistas</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    
    <!-- Leaflet CSS -->
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
    
    <style>
        body {
            margin: 0;
            padding: 0;
            font-family: Arial, sans-serif;
        }
        #map {
            width: 100%;
            height: 100vh;
        }
        .info-panel {
            position: absolute;
            top: 10px;
            right: 10px;
            z-index: 1000;
            background: rgba(255, 255, 255, 0.9);
            padding: 15px;
            border-radius: 5px;
            box-shadow: 0 0 15px rgba(0,0,0,0.2);
            max-width: 300px;
        }
        .route-controls {
            position: absolute;
            bottom: 20px;
            left: 20px;
            z-index: 1000;
            background: rgba(255, 255, 255, 0.9);
            padding: 15px;
            border-radius: 5px;
            box-shadow: 0 0 15px rgba(0,0,0,0.2);
        }
        #route-select {
            padding: 8px;
            min-width: 200px;
        }
        .legend {
            margin-top: 15px;
            line-height: 1.5;
        }
        .legend i {
            width: 20px;
            height: 10px;
            display: inline-block;
            margin-right: 8px;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    
    <div class="info-panel">
        <h3>Rutas Ciclistas</h3>
        <div id="route-info">
            <p>Seleccione una ruta para visualizarla</p>
        </div>
        <div class="legend">
            <h4>Leyenda</h4>
            <p><i style="background:#3498db"></i> Tipo 1-b</p>
            <p><i style="background:#e74c3c"></i> Tipo 2-a/b</p>
            <p><i style="background:#2ecc71"></i> Tipo 3-a/f</p>
            <p><i style="background:#9b59b6"></i> Tipo 4</p>
            <p><i style="background:#f39c12"></i> Tipo 5</p>
        </div>
    </div>
    
    <div class="route-controls">
        <select id="route-select">
            <option value="">-- Seleccione una ruta --</option>
            <?php foreach ($rutas as $ruta): ?>
                <option value="<?= $ruta['id'] ?>">
                    <?= $ruta['id'] ?>. <?= $ruta['calle'] ?> (<?= $ruta['tipologia'] ?>)
                </option>
            <?php endforeach; ?>
        </select>
    </div>

    <!-- Leaflet JS -->
    <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
    
    <script>
        // Inicializar el mapa
        var map = L.map('map').setView([42.85, -2.67], 13);
        
        // Capa base de OpenStreetMap
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
            maxZoom: 18
        }).addTo(map);

        // Almacenar las rutas y la capa actual
        var rutasData = <?php echo json_encode($rutas, JSON_UNESCAPED_UNICODE); ?>;
        var currentRouteLayer = null;

        // Función para obtener el color según la tipología
        function getRouteColor(tipologia) {
            if (!tipologia) return '#7f8c8d';
            
            if (tipologia.startsWith('1-')) return '#3498db';
            if (tipologia.startsWith('2-')) return '#e74c3c';
            if (tipologia.startsWith('3-')) return '#2ecc71';
            if (tipologia === '4') return '#9b59b6';
            if (tipologia === '5') return '#f39c12';
            
            return '#7f8c8d';
        }

        // Función para dibujar una ruta en el mapa
        function drawRoute(routeId) {
            // Eliminar la ruta anterior si existe
            if (currentRouteLayer) {
                map.removeLayer(currentRouteLayer);
            }
            
            // Buscar la ruta seleccionada
            var selectedRoute = rutasData.find(function(route) {
                return route.id == routeId;
            });
            
            if (selectedRoute && selectedRoute.geojson) {
                try {
                    var geojson = JSON.parse(selectedRoute.geojson);
                    var routeColor = getRouteColor(selectedRoute.tipologia);
                    
                    // Crear y añadir la capa de la ruta
                    currentRouteLayer = L.geoJSON(geojson, {
                        style: {
                            color: routeColor,
                            weight: 6,
                            opacity: 1,
                            fillOpacity: 0.7
                        }
                    }).addTo(map);
                    
                    // Ajustar el mapa para mostrar la ruta
                    map.fitBounds(currentRouteLayer.getBounds(), {
                        padding: [50, 50],
                        maxZoom: 15
                    });
                    
                    // Actualizar el panel de información
                    document.getElementById('route-info').innerHTML = `
                        <h4>${selectedRoute.calle}</h4>
                        <p><strong>Tipo:</strong> ${selectedRoute.tipologia || 'Desconocido'}</p>
                        <p><strong>Longitud:</strong> ${selectedRoute.longitud_metros ? selectedRoute.longitud_metros.toFixed(2) + ' m' : 'No disponible'}</p>
                        <p><strong>ID:</strong> ${selectedRoute.id}</p>
                    `;
                    
                } catch (e) {
                    console.error("Error al dibujar la ruta:", e);
                    alert("Error al mostrar la ruta seleccionada");
                }
            }
        }

        // Manejar el cambio de selección
        document.getElementById('route-select').addEventListener('change', function() {
            if (this.value) {
                drawRoute(this.value);
            } else {
                if (currentRouteLayer) {
                    map.removeLayer(currentRouteLayer);
                    currentRouteLayer = null;
                }
                document.getElementById('route-info').innerHTML = '<p>Seleccione una ruta para visualizarla</p>';
            }
        });

        // Dibujar la primera ruta al cargar (opcional)
        if (rutasData.length > 0) {
            document.getElementById('route-select').value = rutasData[0].id;
            drawRoute(rutasData[0].id);
        }
    </script>
</body>
</html>
