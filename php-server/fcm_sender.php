<?php
class FCMSender {
    private $serviceAccount;
    private $accessToken;
    private $tokenExpiry;
    
    public function __construct($serviceAccountPath) {
        if (!file_exists($serviceAccountPath)) {
            throw new Exception("Archivo de cuenta de servicio no encontrado: ".$serviceAccountPath);
        }
        
        $this->serviceAccount = json_decode(file_get_contents($serviceAccountPath), true);
        if (json_last_error() !== JSON_ERROR_NONE) {
            throw new Exception("Error al decodificar el archivo JSON: ".json_last_error_msg());
        }
    }
    
    public function sendToDevice($deviceToken, $title, $body, $data = []) {
        $message = [
            'message' => [
                'token' => $deviceToken,
                'notification' => [
                    'title' => $title,
                    'body' => $body
                ],
                'data' => $data
            ]
        ];
        
        return $this->sendFcmMessage($message);
    }
    
    private function sendFcmMessage($message) {
        $accessToken = $this->getAccessToken();
        
        $headers = [
            'Authorization: Bearer '.$accessToken,
            'Content-Type: application/json'
        ];
        
        $ch = curl_init();
        curl_setopt_array($ch, [
            CURLOPT_URL => FCM_API_URL,
            CURLOPT_POST => true,
            CURLOPT_HTTPHEADER => $headers,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_POSTFIELDS => json_encode($message),
            CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_VERBOSE => true // Solo para depuración
        ]);
        
        $response = curl_exec($ch);
        
        if (curl_errno($ch)) {
            $error = curl_error($ch);
            curl_close($ch);
            throw new Exception("cURL Error: ".$error);
        }
        
        curl_close($ch);
        return json_decode($response, true);
    }
    
    private function getAccessToken() {
        // Verificar si ya tenemos un token válido
        if ($this->accessToken && time() < $this->tokenExpiry) {
            return $this->accessToken;
        }
        
        // Crear el JWT
        $now = time();
        $jwtHeader = json_encode(['alg' => 'RS256', 'typ' => 'JWT']);
        $jwtClaim = json_encode([
            'iss' => $this->serviceAccount['client_email'],
            'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
            'aud' => $this->serviceAccount['token_uri'],
            'iat' => $now,
            'exp' => $now + 3600
        ]);
        
        $jwtData = $this->base64UrlEncode($jwtHeader).'.'.$this->base64UrlEncode($jwtClaim);
        
        // Firmar el JWT
        if (!openssl_sign($jwtData, $signature, $this->serviceAccount['private_key'], 'SHA256')) {
            throw new Exception("Error al firmar el JWT: ".openssl_error_string());
        }
        
        $jwt = $jwtData.'.'.$this->base64UrlEncode($signature);
        
        // Solicitar token de acceso
        $tokenResponse = $this->makeTokenRequest([
            'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
            'assertion' => $jwt
        ]);
        
        if (!isset($tokenResponse['access_token'])) {
            throw new Exception("No se pudo obtener token de acceso: ".print_r($tokenResponse, true));
        }
        
        $this->accessToken = $tokenResponse['access_token'];
        $this->tokenExpiry = $now + 3500; // 100 segundos antes de la expiración
        
        return $this->accessToken;
    }
    
    private function makeTokenRequest($data) {
        $ch = curl_init($this->serviceAccount['token_uri']);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => http_build_query($data),
            CURLOPT_HTTPHEADER => ['Content-Type: application/x-www-form-urlencoded']
        ]);
        
        $response = curl_exec($ch);
        if (curl_errno($ch)) {
            $error = curl_error($ch);
            curl_close($ch);
            throw new Exception("Error al solicitar token: ".$error);
        }
        
        curl_close($ch);
        return json_decode($response, true);
    }
    
    private function base64UrlEncode($data) {
        return str_replace(['+', '/', '='], ['-', '_', ''], base64_encode($data));
    }
}
