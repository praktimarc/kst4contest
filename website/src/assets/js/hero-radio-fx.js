(() => {
    "use strict";

    const canvas = document.querySelector("[data-hero-radio-fx]");
    if (!(canvas instanceof HTMLCanvasElement)) return;

    const hero = canvas.closest(".hero");
    const ctx = canvas.getContext("2d", { alpha: true, desynchronized: true });
    if (!(hero instanceof HTMLElement) || !ctx) return;

    const reducedMotion = matchMedia("(prefers-reduced-motion: reduce)");
    const coarsePointer = matchMedia("(pointer: coarse)");

    const state = {
        width: 0,
        height: 0,
        dpr: 1,
        running: false,
        visible: true,
        lastFrame: 0,
        spawnTime: 0,
        beamTime: 0,
        frameId: 0,
        pointer: { x: 0, y: 0, active: false },
        planes: [],
        beams: [],
        reflections: []
    };

    const clamp = (v, min, max) => Math.max(min, Math.min(max, v));
    const random = (min, max) => min + Math.random() * (max - min);

    function css(name, fallback) {
        return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
    }

    function palette() {
        return {
            plane: css("--fx-plane", "#79ff73"),
            planeMuted: css("--fx-plane-muted", "rgba(121,255,115,.42)"),
            beam: css("--fx-beam", "#52ff65"),
            beamGlow: css("--fx-beam-glow", "rgba(82,255,101,.28)"),
            reflection: css("--fx-reflection", "#d2ffc8"),
            trail: css("--fx-trail", "rgba(121,255,115,.17)"),
            grid: css("--fx-grid", "rgba(121,255,115,.055)")
        };
    }

    function roundedRect(x, y, width, height, radius) {
        const r = Math.min(radius, width / 2, height / 2);
        ctx.beginPath();
        ctx.moveTo(x + r, y);
        ctx.arcTo(x + width, y, x + width, y + height, r);
        ctx.arcTo(x + width, y + height, x, y + height, r);
        ctx.arcTo(x, y + height, x, y, r);
        ctx.arcTo(x, y, x + width, y, r);
        ctx.closePath();
    }

    function resize() {
        const rect = hero.getBoundingClientRect();
        state.width = Math.max(1, Math.round(rect.width));
        state.height = Math.max(1, Math.round(rect.height));
        state.dpr = Math.min(devicePixelRatio || 1, 1.5);
        canvas.width = Math.round(state.width * state.dpr);
        canvas.height = Math.round(state.height * state.dpr);
        canvas.style.width = `${state.width}px`;
        canvas.style.height = `${state.height}px`;
        ctx.setTransform(state.dpr, 0, 0, state.dpr, 0, 0);

        if (!state.pointer.active) {
            state.pointer.x = state.width * 0.55;
            state.pointer.y = state.height * 0.42;
        }
    }

    function stations() {
        const targetX = state.width * 0.54;
        const targetY = state.height * 0.43;
        const left = { x: state.width * 0.075, y: state.height * 0.82, label: "JO51IJ" };
        const right = { x: state.width * 0.925, y: state.height * 0.80, label: "JN49FL" };
        left.angle = Math.atan2(targetY - left.y, targetX - left.x);
        right.angle = Math.atan2(targetY - right.y, targetX - right.x);
        return [left, right];
    }

    function planeLimit() {
        if (coarsePointer.matches) return 5;
        return state.width < 900 ? 7 : 12;
    }

    function createPlane() {
        const margin = 55;
        const edge = Math.floor(Math.random() * 4);
        let x;
        let y;

        if (edge === 0) {
            x = random(0, state.width);
            y = -margin;
        } else if (edge === 1) {
            x = state.width + margin;
            y = random(0, state.height);
        } else if (edge === 2) {
            x = random(0, state.width);
            y = state.height + margin;
        } else {
            x = -margin;
            y = random(0, state.height);
        }

        const tx = state.pointer.active ? state.pointer.x : state.width * random(0.38, 0.72);
        const ty = state.pointer.active ? state.pointer.y : state.height * random(0.20, 0.72);
        const angle = Math.atan2(ty - y, tx - x);
        const speed = random(18, 34);

        return {
            x, y, angle,
            vx: Math.cos(angle) * speed,
            vy: Math.sin(angle) * speed,
            size: random(5.5, 9),
            age: 0,
            maxAge: random(12, 20),
            steering: random(0.65, 1.2),
            wobble: random(0, Math.PI * 2),
            wobbleSpeed: random(0.7, 1.4),
            hit: 0
        };
    }

    function updatePlane(plane, dt) {
        plane.age += dt;
        plane.wobble += dt * plane.wobbleSpeed;

        const tx = state.pointer.active ? state.pointer.x : state.width * 0.56;
        const ty = state.pointer.active ? state.pointer.y : state.height * 0.44;
        const desired = Math.atan2(ty - plane.y, tx - plane.x);
        let diff = desired - plane.angle;
        while (diff > Math.PI) diff -= Math.PI * 2;
        while (diff < -Math.PI) diff += Math.PI * 2;
        plane.angle += diff * plane.steering * dt;

        const speed = Math.hypot(plane.vx, plane.vy);
        const wobble = Math.sin(plane.wobble) * 0.14;
        plane.vx = Math.cos(plane.angle + wobble) * speed;
        plane.vy = Math.sin(plane.angle + wobble) * speed;
        plane.x += plane.vx * dt;
        plane.y += plane.vy * dt;
        plane.hit = Math.max(0, plane.hit - dt * 1.8);
    }

    function expired(plane) {
        const margin = 180;
        return plane.age > plane.maxAge || plane.x < -margin || plane.x > state.width + margin || plane.y < -margin || plane.y > state.height + margin;
    }

    function beamTarget() {
        if (!state.planes.length) return null;
        const tx = state.pointer.active ? state.pointer.x : state.width * 0.56;
        const ty = state.pointer.active ? state.pointer.y : state.height * 0.44;
        const nearby = state.planes
            .map(plane => ({ plane, distance: Math.hypot(plane.x - tx, plane.y - ty) }))
            .filter(item => item.distance < 280)
            .sort((a, b) => a.distance - b.distance);
        return nearby[0]?.plane || state.planes[Math.floor(Math.random() * state.planes.length)];
    }

    function createBeam(target) {
        const source = stations()
            .map(station => ({ station, distance: Math.hypot(target.x - station.x, target.y - station.y) }))
            .sort((a, b) => a.distance - b.distance)[0].station;

        const angle = Math.atan2(target.y - source.y, target.x - source.x);
        const tip = 45;
        state.beams.push({
            startX: source.x + Math.cos(angle) * tip,
            startY: source.y + Math.sin(angle) * tip,
            endX: target.x,
            endY: target.y,
            age: 0,
            maxAge: 0.72
        });

        target.hit = 1;
        const values = [50, 75, 100];
        state.reflections.push({
            x: target.x,
            y: target.y,
            age: 0,
            maxAge: 1.25,
            radius: random(8, 13),
            probability: values[Math.floor(Math.random() * values.length)]
        });
    }

    function updateEffects(dt) {
        state.beams.forEach(beam => beam.age += dt);
        state.beams = state.beams.filter(beam => beam.age < beam.maxAge);
        state.reflections.forEach(r => {
            r.age += dt;
            r.radius += dt * 30;
        });
        state.reflections = state.reflections.filter(r => r.age < r.maxAge);
    }

    function drawGrid(p) {
        ctx.save();
        ctx.strokeStyle = p.grid;
        ctx.lineWidth = 1;
        const spacing = 54;
        for (let x = spacing; x < state.width; x += spacing) {
            ctx.beginPath();
            ctx.moveTo(x, 0);
            ctx.lineTo(x, state.height);
            ctx.stroke();
        }
        for (let y = spacing; y < state.height; y += spacing) {
            ctx.beginPath();
            ctx.moveTo(0, y);
            ctx.lineTo(state.width, y);
            ctx.stroke();
        }
        ctx.restore();
    }

    function drawRadar(p) {
        const x = state.pointer.active ? state.pointer.x : state.width * 0.56;
        const y = state.pointer.active ? state.pointer.y : state.height * 0.44;
        ctx.save();
        ctx.strokeStyle = p.planeMuted;
        ctx.lineWidth = 1;
        for (const radius of [24, 48, 82]) {
            ctx.globalAlpha = 0.24 - radius / 500;
            ctx.beginPath();
            ctx.arc(x, y, radius, 0, Math.PI * 2);
            ctx.stroke();
        }
        ctx.globalAlpha = 0.2;
        ctx.beginPath();
        ctx.moveTo(x - 100, y);
        ctx.lineTo(x + 100, y);
        ctx.moveTo(x, y - 100);
        ctx.lineTo(x, y + 100);
        ctx.stroke();
        ctx.restore();
    }

    function drawYagi(station, p) {
        ctx.save();
        ctx.translate(station.x, station.y);
        ctx.rotate(station.angle);
        ctx.strokeStyle = p.plane;
        ctx.fillStyle = p.plane;
        ctx.lineWidth = 1.5;
        ctx.lineCap = "round";
        ctx.shadowColor = p.beamGlow;
        ctx.shadowBlur = 9;

        ctx.beginPath();
        ctx.moveTo(-30, 26);
        ctx.lineTo(-30, -5);
        ctx.stroke();

        ctx.beginPath();
        ctx.moveTo(-32, 0);
        ctx.lineTo(43, 0);
        ctx.stroke();

        for (const element of [
            { x: -27, length: 34 },
            { x: -12, length: 28 },
            { x: 2, length: 22 },
            { x: 14, length: 19 },
            { x: 25, length: 16 },
            { x: 35, length: 13 }
        ]) {
            ctx.beginPath();
            ctx.moveTo(element.x, -element.length / 2);
            ctx.lineTo(element.x, element.length / 2);
            ctx.stroke();
        }

        ctx.beginPath();
        ctx.arc(-12, 0, 3.2, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        ctx.save();
        ctx.font = "700 11px Inter, system-ui, sans-serif";
        const width = ctx.measureText(station.label).width + 14;
        const x = station.x - width / 2;
        const y = station.y + 32;
        ctx.fillStyle = "rgba(6,18,10,.86)";
        ctx.strokeStyle = p.planeMuted;
        roundedRect(x, y, width, 23, 6);
        ctx.fill();
        ctx.stroke();
        ctx.fillStyle = p.plane;
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        ctx.fillText(station.label, station.x, y + 11.5);
        ctx.restore();
    }

    function drawPlane(plane, p) {
        ctx.save();
        ctx.translate(plane.x, plane.y);
        ctx.rotate(plane.angle);
        ctx.globalAlpha = clamp(Math.min(plane.age * 1.8, (plane.maxAge - plane.age) * 1.8), 0, 1);
        ctx.strokeStyle = p.trail;
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(-plane.size * 4.5, 0);
        ctx.lineTo(-plane.size * 1.4, 0);
        ctx.stroke();

        ctx.fillStyle = plane.hit > 0 ? p.reflection : p.plane;
        ctx.shadowColor = plane.hit > 0 ? p.reflection : p.beamGlow;
        ctx.shadowBlur = plane.hit > 0 ? 16 : 5;
        const s = plane.size;
        ctx.beginPath();
        ctx.moveTo(s * 1.65, 0);
        ctx.lineTo(-s * 0.7, s * 0.34);
        ctx.lineTo(-s * 0.15, s * 1.05);
        ctx.lineTo(-s * 0.65, s * 1.08);
        ctx.lineTo(-s * 1.35, s * 0.3);
        ctx.lineTo(-s * 1.7, s * 0.28);
        ctx.lineTo(-s * 1.12, 0);
        ctx.lineTo(-s * 1.7, -s * 0.28);
        ctx.lineTo(-s * 1.35, -s * 0.3);
        ctx.lineTo(-s * 0.65, -s * 1.08);
        ctx.lineTo(-s * 0.15, -s * 1.05);
        ctx.lineTo(-s * 0.7, -s * 0.34);
        ctx.closePath();
        ctx.fill();
        ctx.restore();
    }

    function drawBeam(beam, p) {
        const progress = beam.age / beam.maxAge;
        const alpha = Math.sin(progress * Math.PI);
        ctx.save();
        ctx.globalAlpha = alpha;
        ctx.strokeStyle = p.beamGlow;
        ctx.lineWidth = 8;
        ctx.shadowColor = p.beam;
        ctx.shadowBlur = 15;
        ctx.setLineDash([10, 6]);
        ctx.lineDashOffset = -progress * 36;
        ctx.beginPath();
        ctx.moveTo(beam.startX, beam.startY);
        ctx.lineTo(beam.endX, beam.endY);
        ctx.stroke();
        ctx.setLineDash([]);
        ctx.strokeStyle = p.beam;
        ctx.lineWidth = 1.8;
        ctx.shadowBlur = 5;
        ctx.beginPath();
        ctx.moveTo(beam.startX, beam.startY);
        ctx.lineTo(beam.endX, beam.endY);
        ctx.stroke();
        ctx.restore();
    }

    function drawReflection(r, p) {
        const progress = r.age / r.maxAge;
        const alpha = 1 - progress;
        ctx.save();
        ctx.translate(r.x, r.y);
        ctx.globalAlpha = alpha;
        ctx.strokeStyle = p.reflection;
        ctx.lineWidth = 1.6;
        ctx.shadowColor = p.beam;
        ctx.shadowBlur = 18;
        for (const scale of [1, 1.65]) {
            ctx.globalAlpha = alpha * (scale === 1 ? 1 : 0.55);
            ctx.beginPath();
            ctx.arc(0, 0, r.radius * scale, 0, Math.PI * 2);
            ctx.stroke();
        }
        ctx.globalAlpha = alpha * 0.8;
        const cross = r.radius * 1.25;
        ctx.beginPath();
        ctx.moveTo(-cross, 0);
        ctx.lineTo(cross, 0);
        ctx.moveTo(0, -cross);
        ctx.lineTo(0, cross);
        ctx.stroke();
        ctx.globalAlpha = alpha;
        ctx.fillStyle = p.reflection;
        ctx.beginPath();
        ctx.arc(0, 0, 3.8, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        ctx.save();
        const label = `${r.probability}%`;
        ctx.font = "800 12px Inter, system-ui, sans-serif";
        const width = ctx.measureText(label).width + 16;
        const height = 25;
        const x = r.x + r.radius + 12;
        const y = r.y - r.radius - 17;
        ctx.globalAlpha = clamp(alpha * 1.25, 0, 1);
        ctx.fillStyle = "rgba(6,18,10,.91)";
        ctx.strokeStyle = p.plane;
        ctx.shadowColor = p.beamGlow;
        ctx.shadowBlur = 10;
        roundedRect(x, y, width, height, 6);
        ctx.fill();
        ctx.stroke();
        ctx.shadowBlur = 0;
        ctx.fillStyle = p.reflection;
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        ctx.fillText(label, x + width / 2, y + height / 2);
        ctx.restore();
    }

    function draw(staticOnly = false) {
        const p = palette();
        ctx.clearRect(0, 0, state.width, state.height);
        drawGrid(p);
        drawRadar(p);
        stations().forEach(station => drawYagi(station, p));
        if (staticOnly) return;
        state.planes.forEach(plane => drawPlane(plane, p));
        state.beams.forEach(beam => drawBeam(beam, p));
        state.reflections.forEach(r => drawReflection(r, p));
    }

    function animate(timestamp) {
        if (!state.running) return;
        const dt = clamp((timestamp - state.lastFrame) / 1000 || 0, 0, 0.05);
        state.lastFrame = timestamp;

        if (!state.visible || document.hidden) {
            state.frameId = requestAnimationFrame(animate);
            return;
        }

        state.spawnTime += dt;
        state.beamTime += dt;
        const spawnInterval = coarsePointer.matches ? 2.2 : 1.15;
        const beamInterval = coarsePointer.matches ? 3.3 : 1.8;

        if (state.spawnTime >= spawnInterval && state.planes.length < planeLimit()) {
            state.spawnTime = 0;
            state.planes.push(createPlane());
        }

        if (state.beamTime >= beamInterval && state.planes.length) {
            state.beamTime = 0;
            const target = beamTarget();
            if (target) createBeam(target);
        }

        state.planes.forEach(plane => updatePlane(plane, dt));
        state.planes = state.planes.filter(plane => !expired(plane));
        updateEffects(dt);
        draw(false);
        state.frameId = requestAnimationFrame(animate);
    }

    function start() {
        cancelAnimationFrame(state.frameId);
        resize();
        if (reducedMotion.matches) {
            state.running = false;
            draw(true);
            return;
        }

        state.running = true;
        state.lastFrame = performance.now();
        if (!state.planes.length) {
            const count = coarsePointer.matches ? 2 : 5;
            for (let i = 0; i < count; i++) {
                const plane = createPlane();
                plane.age = Math.random() * 2;
                state.planes.push(plane);
            }
        }
        state.frameId = requestAnimationFrame(animate);
    }

    hero.addEventListener("pointermove", event => {
        const rect = hero.getBoundingClientRect();
        state.pointer.x = clamp(event.clientX - rect.left, 0, rect.width);
        state.pointer.y = clamp(event.clientY - rect.top, 0, rect.height);
        state.pointer.active = true;
    }, { passive: true });

    hero.addEventListener("pointerleave", () => state.pointer.active = false);
    addEventListener("resize", resize, { passive: true });
    document.addEventListener("visibilitychange", () => state.lastFrame = performance.now());
    reducedMotion.addEventListener("change", start);

    new ResizeObserver(() => {
        resize();
        if (reducedMotion.matches) draw(true);
    }).observe(hero);

    new IntersectionObserver(entries => {
        state.visible = entries[0]?.isIntersecting ?? true;
        state.lastFrame = performance.now();
    }, { threshold: 0.05 }).observe(hero);

    start();
})();
