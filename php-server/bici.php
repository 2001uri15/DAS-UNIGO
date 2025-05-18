<?php
// Conexión a la base de datos
$host = 'localhost';
$dbname = 'Xalarrazabal025_unigo';
$user = 'Xalarrazabal025';
$password = 'vRN7UMCCFV';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8", $user, $password);
} catch (PDOException $e) {
    die("Error de conexión: " . $e->getMessage());
}

// Consulta los datos de rutas de bici solo con bici_ruta
$sql = "SELECT id, lat, lon 
        FROM bici_ruta 
        ORDER BY id, lon, lat";

$stmt = $pdo->prepare($sql);
$stmt->execute();

$bikeRoutes = [];
while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
    $bikeRoutes[$row['id']][] = [
        'lat' => (float)$row['lat'],
        'lon' => (float)$row['lon']
    ];
}
?>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Mapa de Carriles Bici - Vitoria-Gasteiz</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
    <style>
        #map { height: 100vh; width: 100%; }
        .info {
            padding: 6px 8px;
            font: 14px/16px Arial, Helvetica, sans-serif;
            background: white;
            background: rgba(255,255,255,0.8);
            box-shadow: 0 0 15px rgba(0,0,0,0.2);
            border-radius: 5px;
        }
        .legend {
            line-height: 18px;
            color: #555;
        }
        .legend i {
            width: 18px;
            height: 18px;
            float: left;
            margin-right: 8px;
            opacity: 0.7;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <script>
        // Centro del mapa en Vitoria-Gasteiz
        const map = L.map('map').setView([42.8485668, -2.6767569], 13);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);

        // Colores para diferentes tipos de carriles bici
        const colors = {
            '1-b': '#3498db',  // Azul para tipo 1-b
            '5': '#2ecc71',    // Verde para tipo 5
            '3-a': '#e74c3c',  // Rojo para tipo 3-a
            '3-f': '#9b59b6',  // Morado para tipo 3-f
            '2-a': '#f39c12',  // Naranja para tipo 2-a
            'default': '#34495e' // Gris para otros
        };

        const rutasBici = <?php echo json_encode($bikeRoutes); ?>;
        
        // Crear control de leyenda
        const legend = L.control({position: 'bottomright'});
        
        legend.onAdd = function(map) {
            const div = L.DomUtil.create('div', 'info legend');
            const tipos = ['1-b', '5', '3-a', '3-f', '2-a'];
            
            div.innerHTML = '<h4>Tipología</h4>';
            
            for (let i = 0; i < tipos.length; i++) {
                div.innerHTML += 
                    '<i style="background:' + colors[tipos[i]] + '"></i> ' +
                    tipos[i] + '<br>';
            }
            
            return div;
        };
        
        legend.addTo(map);

        // Dibujar las rutas
        for (const [rutaId, puntos] of Object.entries(rutasBici)) {
            if (puntos.length < 2) continue; // Necesitamos al menos 2 puntos para una línea
            
            const latlngs = puntos.map(p => [p.lat, p.lon]);
            
            // Obtener el color según el tipo de ruta (simplificado)
            // En una implementación real, deberías consultar el tipo de cada ruta
            const color = colors['default'];
            
            L.polyline(latlngs, { 
                color: color, 
                weight: 4,
                opacity: 0.7,
                lineJoin: 'round'
            }).addTo(map)
            .bindPopup("Carril bici ID: " + rutaId);
        }
    </script>
</body>
</html>