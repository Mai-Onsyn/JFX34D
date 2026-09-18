module;
export module Data4D;
import Vectors;
import Color;

#pragma pack(push, 1)
export struct Vertex3D {
    Vector3D pos;
    Color color;
    Vector3D normal;
};

export struct Vertex4D {
    Vector4D pos;
    Color color;
    Vector4D normal;
};

export struct ClipVertex4D {
    Vector5D pos;
    Color color;
    Vector4D normal;
};

export struct Tetrahedron3D {
    Vertex3D vertices[4];
};

export struct Tetrahedron4D {
    Vertex4D vertices[4];
};
#pragma pack(pop)