(function () {
    if (typeof enableSiteStyleSheets === 'function') enableSiteStyleSheets();
    if (typeof document.innerHTMLCache !== 'string') return;

    document.body.innerHTML = document.innerHTMLCache;
    document.body.classList.remove('mozac-readerview-body');

    const target = document.querySelector('[data-einkbro-reader-source-end]');
    if (!target) {
        window.scrollTo(0, document.documentElement.scrollHeight);
        return;
    }

    target.removeAttribute('data-einkbro-reader-source-end');
    requestAnimationFrame(function () {
        requestAnimationFrame(function () {
            target.scrollIntoView({ block: 'end', inline: 'nearest' });
        });
    });
})();
