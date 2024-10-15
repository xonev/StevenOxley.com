async function handler(event) {
  const request = event.request;
  const canonicalHost = 'www.stevenoxley.com';
  const alternateHost = 'stevenoxley.com';

  console.log(JSON.stringify(event));

  if (request.headers.host && request.headers.host.value === alternateHost) {
    const response = {
      statusCode: 301,
      statusDescription: 'Moved Permanently',
      headers: {
        location: { value: 'https://' + canonicalHost + request.uri }
      }
    };
    return response;
  }
  if (request.uri.endsWith('/index.html')) {
    const modifiedRequestURI = request.uri.replace(/index.html$/, '');
    const response = {
      statusCode: 301,
      statusDescription: 'Moved Permanently',
      headers: {
        location: { value: 'https://' + canonicalHost + modifiedRequestURI }
      }
    };
    return response;
  }
  if (request.uri.match(/\/[^/.]+$/)) {
    // redirect paths like https://www.stevenoxley.com/this to https://www.stevenoxley.com/this/
    const response = {
      statusCode: 301,
      statusDescription: 'Moved Permanently',
      headers: {
        location: { value: 'https://' + canonicalHost + request.uri + '/' }
      }
    };
    return response;
  }

  if (request.uri.match('.+/$')) {
    request.uri += 'index.html';
  }
  return request;

}
