export type Vec3 = readonly [number, number, number];
/** 3x3 rotation matrix, row by row. */
export type Mat3 = readonly [Vec3, Vec3, Vec3];

export interface D20Face {
  value: number;
  normal: Vec3;
  /** Rotation that turns this face toward the viewer with its number upright. */
  landing: Mat3;
  /** CSS matrix3d placing the flat triangle element on the die. */
  transform: string;
}

const PHI = (1 + Math.sqrt(5)) / 2;
const VERTICES: Vec3[] = [
  [-1, PHI, 0], [1, PHI, 0], [-1, -PHI, 0], [1, -PHI, 0],
  [0, -1, PHI], [0, 1, PHI], [0, -1, -PHI], [0, 1, -PHI],
  [PHI, 0, -1], [PHI, 0, 1], [-PHI, 0, -1], [-PHI, 0, 1],
];
const TRIANGLES: Vec3[] = [
  [0, 11, 5], [0, 5, 1], [0, 1, 7], [0, 7, 10], [0, 10, 11],
  [1, 5, 9], [5, 11, 4], [11, 10, 2], [10, 7, 6], [7, 1, 8],
  [3, 9, 4], [3, 4, 2], [3, 2, 6], [3, 6, 8], [3, 8, 9],
  [4, 9, 5], [2, 4, 11], [6, 2, 10], [8, 6, 7], [9, 8, 1],
];

export const dot = (a: Vec3, b: Vec3): number => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
const sub = (a: Vec3, b: Vec3): Vec3 => [a[0] - b[0], a[1] - b[1], a[2] - b[2]];
const scale = (a: Vec3, s: number): Vec3 => [a[0] * s, a[1] * s, a[2] * s];
const cross = (a: Vec3, b: Vec3): Vec3 => [
  a[1] * b[2] - a[2] * b[1],
  a[2] * b[0] - a[0] * b[2],
  a[0] * b[1] - a[1] * b[0],
];
export const normalize = (a: Vec3): Vec3 => scale(a, 1 / Math.sqrt(dot(a, a)));

export const apply = (m: Mat3, v: Vec3): Vec3 => [dot(m[0], v), dot(m[1], v), dot(m[2], v)];

export function multiply(a: Mat3, b: Mat3): Mat3 {
  const row = (r: Vec3): Vec3 => [
    r[0] * b[0][0] + r[1] * b[1][0] + r[2] * b[2][0],
    r[0] * b[0][1] + r[1] * b[1][1] + r[2] * b[2][1],
    r[0] * b[0][2] + r[1] * b[1][2] + r[2] * b[2][2],
  ];
  return [row(a[0]), row(a[1]), row(a[2])];
}

export function rotation(axis: Vec3, angle: number): Mat3 {
  const [x, y, z] = axis;
  const c = Math.cos(angle);
  const s = Math.sin(angle);
  const t = 1 - c;
  return [
    [t * x * x + c, t * x * y - s * z, t * x * z + s * y],
    [t * x * y + s * z, t * y * y + c, t * y * z - s * x],
    [t * x * z - s * y, t * y * z + s * x, t * z * z + c],
  ];
}

export function cssMatrix(m: Mat3, translate: Vec3 = [0, 0, 0]): string {
  const [t0, t1, t2] = translate;
  return `matrix3d(${m[0][0]},${m[1][0]},${m[2][0]},0,${m[0][1]},${m[1][1]},${m[2][1]},0,${m[0][2]},${m[1][2]},${m[2][2]},0,${t0},${t1},${t2},1)`;
}

/** Builds the die for triangle elements of the given size. Opposite faces add up to 21, as on a real d20. */
export function buildD20(faceWidth: number, faceHeight: number, edge: number): D20Face[] {
  const radius = edge * 0.951;
  const points = VERTICES.map((v) => scale(normalize(v), radius));

  const faces = TRIANGLES.map(([a, b, c]) => {
    const [pa, pb, pc] = [points[a], points[b], points[c]];
    const center: Vec3 = scale([pa[0] + pb[0] + pc[0], pa[1] + pb[1] + pc[1], pa[2] + pb[2] + pc[2]], 1 / 3);
    const z = normalize(center);
    const y = normalize(sub(center, pa));
    const x = cross(y, z);
    // The element's centroid sits at (width / 2, height * 2 / 3) from its top-left corner.
    const offset: Vec3 = [
      center[0] - x[0] * faceWidth / 2 - y[0] * faceHeight * 2 / 3,
      center[1] - x[1] * faceWidth / 2 - y[1] * faceHeight * 2 / 3,
      center[2] - x[2] * faceWidth / 2 - y[2] * faceHeight * 2 / 3,
    ];
    const placement: Mat3 = [
      [x[0], y[0], z[0]],
      [x[1], y[1], z[1]],
      [x[2], y[2], z[2]],
    ];
    return {
      value: 0,
      normal: z,
      landing: [x, y, z] as Mat3,
      transform: cssMatrix(placement, offset),
    };
  });

  let next = 1;
  for (const face of faces) {
    if (face.value === 0) {
      face.value = next;
      faces.find((other) => dot(other.normal, face.normal) < -0.99)!.value = 21 - next;
      next++;
    }
  }
  return faces;
}
