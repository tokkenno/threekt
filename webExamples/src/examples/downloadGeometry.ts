export function downloadGeometry(data: string | Int8Array, name: string) {
    const blob = typeof data === 'string'
        ? new Blob([data], { type: 'text/plain' })
        : new Blob([data.slice()], { type: 'application/octet-stream' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = name;
    link.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}
