const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');

const { parseEventFrame } = require(path.resolve(__dirname, '../../main/resources/static/assets/sse.js'));

test('preserves token-leading spaces in SSE data lines', () => {
  const parsed = parseEventFrame('event: token\ndata:  world');

  assert.deepEqual(parsed, {
    event: 'token',
    data: ' world'
  });
});

test('keeps JSON event payloads unchanged apart from the SSE separator', () => {
  const parsed = parseEventFrame('event: complete\ndata: {"reply":"hello"}');

  assert.deepEqual(parsed, {
    event: 'complete',
    data: '{"reply":"hello"}'
  });
});
