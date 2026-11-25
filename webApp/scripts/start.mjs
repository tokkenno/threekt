import { execFileSync } from 'node:child_process';
import { X509Certificate } from 'node:crypto';
import { mkdirSync, readFileSync } from 'node:fs';
import { networkInterfaces } from 'node:os';
import { resolve } from 'node:path';
import { createServer } from 'vite';

const certificateDirectory = resolve('node_modules/.cache/threekt-https');
const suppliedCert = process.env.THREEKT_HTTPS_CERT;
const suppliedKey = process.env.THREEKT_HTTPS_KEY;

function httpsOptions() {
    if (suppliedCert || suppliedKey) {
        if (!suppliedCert || !suppliedKey) {
            throw new Error('Set both THREEKT_HTTPS_CERT and THREEKT_HTTPS_KEY to use your own certificate.');
        }
        return { cert: readFileSync(suppliedCert), key: readFileSync(suppliedKey) };
    }

    const addresses = [...new Set(['127.0.0.1', ...Object.values(networkInterfaces()).flat()
        .filter(address => address && address.family === 'IPv4' && !address.internal)
        .map(address => address.address)])].sort();
    const certPath = resolve(certificateDirectory, 'cert.pem');
    const keyPath = resolve(certificateDirectory, 'key.pem');

    try {
        const cert = readFileSync(certPath);
        const key = readFileSync(keyPath);
        const certificate = new X509Certificate(cert);
        if (Date.parse(certificate.validTo) > Date.now() && addresses.every(address => certificate.checkIP(address))) {
            return { cert, key };
        }
    } catch (error) {
        if (error.code !== 'ENOENT') throw error;
    }

    mkdirSync(certificateDirectory, { recursive: true, mode: 0o700 });
    const names = ['DNS:localhost', ...addresses.map(address => `IP:${address}`)].join(',');
    execFileSync('openssl', [
        'req', '-x509', '-newkey', 'rsa:2048', '-sha256', '-nodes', '-days', '365',
        '-subj', '/CN=localhost', '-addext', `subjectAltName=${names}`,
        '-keyout', keyPath, '-out', certPath,
    ], { stdio: ['ignore', 'ignore', 'pipe'] });
    return { cert: readFileSync(certPath), key: readFileSync(keyPath) };
}

const shared = { host: '0.0.0.0', strictPort: true };
const servers = [];
try {
    const https = httpsOptions();
    for (const options of [
        { server: { ...shared, port: 8080 } },
        {
            cacheDir: 'node_modules/.vite-https',
            server: { ...shared, port: 8443, https },
        },
    ]) {
        const server = await createServer(options);
        servers.push(server);
        await server.listen();
        server.printUrls();
    }
    if (!suppliedCert) console.log(`Local HTTPS certificate: ${resolve(certificateDirectory, 'cert.pem')}`);
} catch (error) {
    await Promise.allSettled(servers.map(server => server.close()));
    throw error;
}
