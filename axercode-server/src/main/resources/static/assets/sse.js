(function (globalScope) {
    function parseEventFrame(frame) {
        const lines = frame.split('\n');
        let name = null;
        const data = [];

        for (const line of lines) {
            if (line.startsWith('event:')) {
                name = line.slice(6).trim();
                continue;
            }

            if (line.startsWith('data:')) {
                data.push(readDataValue(line));
            }
        }

        if (!name) {
            return null;
        }

        return {
            event: name,
            data: data.join('\n')
        };
    }

    function readDataValue(line) {
        let value = line.slice(5);
        if (value.startsWith(' ')) {
            value = value.slice(1);
        }
        return value;
    }

    const api = { parseEventFrame, readDataValue };
    if (typeof module !== 'undefined' && module.exports) {
        module.exports = api;
    }
    globalScope.AxerCodeSse = api;
})(typeof globalThis !== 'undefined' ? globalThis : this);
