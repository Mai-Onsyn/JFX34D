package weilantianhai.agent.ir.resolver;

import weilantianhai.agent.ir.operations.IOperation;
import weilantianhai.agent.ir.IRException;
import weilantianhai.agent.ir.operations.AddTetrahedronOperation;
import weilantianhai.agent.ir.operations.ApplyTransformToVertexOperation;
import weilantianhai.agent.ir.operations.CopyModelOperation;
import weilantianhai.agent.ir.operations.Create16CellOperation;
import weilantianhai.agent.ir.operations.Create5CellOperation;
import weilantianhai.agent.ir.operations.CreateBall4Operation;
import weilantianhai.agent.ir.operations.CreateCone4Operation;
import weilantianhai.agent.ir.operations.CreateModelOperation;
import weilantianhai.agent.ir.operations.CreatePrism4Operation;
import weilantianhai.agent.ir.operations.CreateTesseractOperation;
import weilantianhai.agent.ir.operations.CreateTetrahedronOperation;
import weilantianhai.agent.ir.operations.DeleteModelOperation;
import weilantianhai.agent.ir.operations.GetCameraPosOperation;
import weilantianhai.agent.ir.operations.GetCameraViewOperation;
import weilantianhai.agent.ir.operations.GetModelInfoOperation;
import weilantianhai.agent.ir.operations.GetModelMatrixOperation;
import weilantianhai.agent.ir.operations.GetTetrahedronOperation;
import weilantianhai.agent.ir.operations.ListModelOperation;
import weilantianhai.agent.ir.operations.MergeAllSubModelsOperation;
import weilantianhai.agent.ir.operations.MergeModelOperation;
import weilantianhai.agent.ir.operations.MoveCameraPosOperation;
import weilantianhai.agent.ir.operations.RemoveTetrahedronOperation;
import weilantianhai.agent.ir.operations.RenameModelOperation;
import weilantianhai.agent.ir.operations.RotateCameraViewOperation;
import weilantianhai.agent.ir.operations.SetCameraPosOperation;
import weilantianhai.agent.ir.operations.SetCameraViewOperation;
import weilantianhai.agent.ir.operations.SetModelVisibleOperation;
import weilantianhai.agent.ir.operations.SetTetrahedronVertexOperation;
import weilantianhai.agent.ir.operations.SliceModelOperation;
import weilantianhai.agent.ir.operations.TransformModelOperation;
import weilantianhai.agent.ir.operations.TransformTetrahedronsOperation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 操作注册表：把 JSON 里的 {@code type} 字符串映射到具体的 {@link IOperation} 实例。
 *
 * <p>注册的 {@code type} 必须与对应操作类 {@code name()} 的返回值完全一致，
 * 否则 {@link #creat(String)} 会抛 {@code UNKNOWN_OP}。
 *
 * <p>当前共 **31 个** agent 可调用的操作，分五类：
 * 摄像机 6、模型树 9、模型变换 2、基础形状 7、几何雕刻 7。
 *
 * <p><b>不在这里注册的</b>：{@code IOInterface}（{@code saveModel} / {@code loadModel}）与
 * {@code SceneInterface} 全部方法属于 **UI / 内部调用**，文档明确写了"agent 不应生成"，
 * 所以没有对应操作类；{@code ModelInterface.getModel} 同理（直接返回 {@code Mesh4D} 实例）。
 */
public class OperationRegistry {
    private final Map<String, Supplier<IOperation>> map = new HashMap<>();

    //注册操作
    public OperationRegistry() {
        // ---------- 摄像机：位置 ----------
        register("GET_CAMERA_POS", GetCameraPosOperation::new);
        register("MOVE_CAMERA_POS", MoveCameraPosOperation::new);
        register("SET_CAMERA_POS", SetCameraPosOperation::new);

        // ---------- 摄像机：视角 ----------
        register("GET_CAMERA_VIEW", GetCameraViewOperation::new);
        register("ROTATE_CAMERA_VIEW", RotateCameraViewOperation::new);
        register("SET_CAMERA_VIEW", SetCameraViewOperation::new);

        // ---------- 模型树 ----------
        register("LIST_MODEL", ListModelOperation::new);
        register("CREATE_MODEL", CreateModelOperation::new);
        register("DELETE_MODEL", DeleteModelOperation::new);
        register("COPY_MODEL", CopyModelOperation::new);
        register("MERGE_MODEL", MergeModelOperation::new);
        register("APPLY_TRANSFORM_TO_VERTEX", ApplyTransformToVertexOperation::new);
        register("RENAME_MODEL", RenameModelOperation::new);
        register("MERGE_ALL_SUB_MODELS", MergeAllSubModelsOperation::new);
        register("SET_MODEL_VISIBLE", SetModelVisibleOperation::new);

        // ---------- 模型变换 ----------
        register("GET_MODEL_MATRIX", GetModelMatrixOperation::new);
        register("TRANSFORM_MODEL", TransformModelOperation::new);

        // ---------- 基础形状 ----------
        register("CREATE_TETRAHEDRON", CreateTetrahedronOperation::new);
        register("CREATE_5CELL", Create5CellOperation::new);
        register("CREATE_16CELL", Create16CellOperation::new);
        register("CREATE_TESSERACT", CreateTesseractOperation::new);
        register("CREATE_PRISM4", CreatePrism4Operation::new);
        register("CREATE_CONE4", CreateCone4Operation::new);
        register("CREATE_BALL4", CreateBall4Operation::new);

        // ---------- 模型几何编辑（雕刻） ----------
        register("GET_MODEL_INFO", GetModelInfoOperation::new);
        register("GET_TETRAHEDRON", GetTetrahedronOperation::new);
        register("SET_TETRAHEDRON_VERTEX", SetTetrahedronVertexOperation::new);
        register("TRANSFORM_TETRAHEDRONS", TransformTetrahedronsOperation::new);
        register("ADD_TETRAHEDRON", AddTetrahedronOperation::new);
        register("REMOVE_TETRAHEDRON", RemoveTetrahedronOperation::new);
        register("SLICE_MODEL", SliceModelOperation::new);
    }

    public void register(String type,Supplier<IOperation> supplier){
        map.put(type,supplier);
    }

    public IOperation creat(String type) throws IRException {
        Supplier<IOperation> s = map.get(type);
        if(s == null){
            throw new IRException("UNKNOWN_OP","未知操作类型：" + type);
        }
        return s.get();
    }

    /** 已注册的操作数量，供自检使用 */
    public int size() {
        return map.size();
    }
}
